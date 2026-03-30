package gdscript.extension

import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerState
import godot.lsp.service.GodotLspServerSupportProvider
import org.eclipse.lsp4j.*

data class GdExtMethodInfo(
    val name: String,
    val params: List<Pair<String, String>>,  // name to type
    val returnType: String
)

data class GdExtPropertyInfo(
    val name: String,
    val type: String
)

data class GdExtSignalInfo(
    val name: String
)

data class GdExtTypeInfo(
    val name: String,
    val inherits: String,
    val methods: List<GdExtMethodInfo>,
    val properties: List<GdExtPropertyInfo>,
    val signals: List<GdExtSignalInfo>
)

/**
 * Queries the Godot LSP (via the existing godot-lsp plugin connection) for all
 * available types, then fetches method/property details for GDExtension types.
 */
class GdExtensionTypeCollector(private val project: Project, private val godotProjectPath: String) {

    private val probeUri = "file://$godotProjectPath/_gdext_probe.gd"

    companion object {
        private const val LSP_KIND_METHOD = 2
        private const val LSP_KIND_PROPERTY = 10
        private const val LSP_KIND_SIGNAL = 23 // Event kind
        private const val LSP_KIND_CLASS = 7
    }

    private fun getLspServer(): LspServer? {
        val servers = LspServerManager.getInstance(project)
            .getServersForProvider(GodotLspServerSupportProvider::class.java)
        return servers.firstOrNull { it.state == LspServerState.Running }
    }

    /**
     * Returns all type names available via LSP completion in a type-hint position.
     */
    fun collectAllTypeNames(): List<String> {
        val server = getLspServer()
        if (server == null) {
            thisLogger().warn("No running Godot LSP server found")
            return emptyList()
        }

        openDocument(server, "extends Node\nvar _x: ")

        val result = server.sendRequestSync(5000) { ls ->
            ls.textDocumentService.completion(CompletionParams(
                TextDocumentIdentifier(probeUri),
                Position(1, 8)
            ))
        }

        closeDocument(server)

        if (result == null) {
            thisLogger().warn("LSP completion returned null")
            return emptyList()
        }

        val items = when {
            result.isLeft -> result.left
            result.isRight -> result.right.items
            else -> emptyList()
        }

        thisLogger().info("Extracted ${items.size} completion items")

        if (items.isNotEmpty()) {
            val kinds = items.mapNotNull { it.kind?.value }.distinct().sorted()
            thisLogger().info("Completion item kinds present: $kinds")
        }

        val classItems = items.filter { it.kind?.value == LSP_KIND_CLASS }
        thisLogger().info("Found ${classItems.size} class items (kind=$LSP_KIND_CLASS)")

        return classItems.map { it.label }
    }

    /**
     * Collects full type info (methods, properties, signals) for a given type name.
     */
    fun collectTypeDetails(typeName: String): GdExtTypeInfo {
        val server = getLspServer() ?: return GdExtTypeInfo(typeName, "RefCounted", emptyList(), emptyList(), emptyList())

        // Get inheritance via hover
        val inherits = getInheritance(server, typeName)

        // Open doc with instance and request completion on members
        val doc = "extends Node\nvar _x := $typeName.new()\nfunc _r():\n\t_x."
        openDocument(server, doc)

        val result = server.sendRequestSync(5000) { ls ->
            ls.textDocumentService.completion(CompletionParams(
                TextDocumentIdentifier(probeUri),
                Position(3, 4)
            ))
        }

        closeDocument(server)

        val items = when {
            result == null -> emptyList()
            result.isLeft -> result.left
            result.isRight -> result.right.items
            else -> emptyList()
        }

        val methods = mutableListOf<GdExtMethodInfo>()
        val properties = mutableListOf<GdExtPropertyInfo>()
        val signals = mutableListOf<GdExtSignalInfo>()

        for (item in items) {
            val kind = item.kind?.value ?: continue
            val label = item.label ?: continue

            when (kind) {
                LSP_KIND_METHOD -> {
                    val methodName = label.removeSuffix("(…)").removeSuffix("()")
                    val sig = getMethodSignature(server, typeName, methodName)
                    if (sig != null) {
                        methods.add(sig)
                    }
                }
                LSP_KIND_PROPERTY -> {
                    properties.add(GdExtPropertyInfo(label, "Variant"))
                }
                LSP_KIND_SIGNAL -> {
                    signals.add(GdExtSignalInfo(label))
                }
            }
        }

        return GdExtTypeInfo(typeName, inherits, methods, properties, signals)
    }

    private fun getInheritance(server: LspServer, typeName: String): String {
        openDocument(server, "extends Node\nvar _x: $typeName")

        val result = server.sendRequestSync(5000) { ls ->
            ls.textDocumentService.hover(HoverParams(
                TextDocumentIdentifier(probeUri),
                Position(1, 10)
            ))
        }

        closeDocument(server)

        val value = result?.contents?.right?.value ?: return "RefCounted"
        val extendsMatch = Regex("extends\\s+(\\w+)").find(value)
        return extendsMatch?.groupValues?.get(1) ?: "RefCounted"
    }

    private fun getMethodSignature(server: LspServer, typeName: String, methodName: String): GdExtMethodInfo? {
        openDocument(server, "extends Node\nvar _x := $typeName.new()\nfunc _r():\n\t_x.$methodName()")

        val result = server.sendRequestSync(5000) { ls ->
            ls.textDocumentService.hover(HoverParams(
                TextDocumentIdentifier(probeUri),
                Position(3, 5)
            ))
        }

        closeDocument(server)

        val value = result?.contents?.right?.value ?: return null
        return parseMethodSignature(methodName, value)
    }

    private fun parseMethodSignature(methodName: String, hover: String): GdExtMethodInfo? {
        val sigMatch = Regex("func\\s+\\w+\\.$methodName\\(([^)]*)\\)(?:\\s*->\\s*(\\w[\\w\\[\\]]*))?" ).find(hover)
            ?: return GdExtMethodInfo(methodName, emptyList(), "void")

        val paramsStr = sigMatch.groupValues[1].trim()
        val returnType = sigMatch.groupValues[2].ifBlank { "void" }

        val params = if (paramsStr.isEmpty()) {
            emptyList()
        } else {
            paramsStr.split(",").map { param ->
                val parts = param.trim().split(":")
                val pName = parts[0].trim()
                val pType = if (parts.size > 1) parts[1].trim() else "Variant"
                pName to pType
            }
        }

        return GdExtMethodInfo(methodName, params, returnType)
    }

    private fun openDocument(server: LspServer, text: String) {
        server.sendNotification { ls ->
            ls.textDocumentService.didClose(DidCloseTextDocumentParams(
                TextDocumentIdentifier(probeUri)
            ))
        }

        server.sendNotification { ls ->
            ls.textDocumentService.didOpen(DidOpenTextDocumentParams(
                TextDocumentItem(probeUri, "gdscript", 1, text)
            ))
        }

        // Let the server process
        Thread.sleep(200)
    }

    private fun closeDocument(server: LspServer) {
        server.sendNotification { ls ->
            ls.textDocumentService.didClose(DidCloseTextDocumentParams(
                TextDocumentIdentifier(probeUri)
            ))
        }
    }
}
