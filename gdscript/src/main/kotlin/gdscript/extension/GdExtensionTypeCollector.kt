package gdscript.extension

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerState
import godot.lsp.service.GodotLspServerSupportProvider
import org.eclipse.lsp4j.*
import java.util.concurrent.TimeUnit

data class GdExtMethodInfo(
    val name: String,
    val params: List<Pair<String, String>>,
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

class GdExtensionTypeCollector(private val project: Project, private val godotProjectPath: String) {

    private val probeUri = "file://$godotProjectPath/_gdext_probe.gd"
    private val probeFile = java.io.File(godotProjectPath, "_gdext_probe.gd")

    companion object {
        private const val LSP_KIND_METHOD = 2
        private const val LSP_KIND_PROPERTY = 10
        private const val LSP_KIND_SIGNAL = 23
        private const val LSP_KIND_CLASS = 7
    }

    private fun getRawServer(): org.eclipse.lsp4j.services.LanguageServer? {
        val servers = LspServerManager.getInstance(project)
            .getServersForProvider(GodotLspServerSupportProvider::class.java)
        val server = servers.firstOrNull { it.state == LspServerState.Running } ?: return null
        return try {
            val impl = server as? com.intellij.platform.lsp.impl.LspServerImpl ?: return null
            val method = impl.javaClass.getMethod("getLsp4jServer\$intellij_platform_lsp_impl")
            method.invoke(impl) as? org.eclipse.lsp4j.services.LanguageServer
        } catch (e: Exception) {
            thisLogger().warn("Cannot access raw LSP server: ${e.message}")
            null
        }
    }

    fun collectAllTypeNames(): List<String> {
        val rawServer = getRawServer()
        if (rawServer == null) {
            thisLogger().warn("No running Godot LSP server found")
            return emptyList()
        }

        openDocument(rawServer, "extends ")

        try {
            val result = rawServer.textDocumentService.completion(CompletionParams(
                TextDocumentIdentifier(probeUri), Position(0, 8)
            )).get(5, TimeUnit.SECONDS)

            val items = extractItems(result) ?: emptyList()
            thisLogger().info("Extracted ${items.size} completion items")

            val classItems = items.filter { it.kind?.value == LSP_KIND_CLASS }
            thisLogger().info("Found ${classItems.size} class items")
            return classItems.map { it.label }
        } finally {
            closeDocument(rawServer)
        }
    }

    /**
     * Collect details for multiple types in a single batch.
     * Opens one probe document per type to get member completions.
     */
    fun collectAllTypeDetails(typeNames: List<String>): List<GdExtTypeInfo> {
        val rawServer = getRawServer() ?: return typeNames.map {
            GdExtTypeInfo(it, "RefCounted", emptyList(), emptyList(), emptyList())
        }

        val results = mutableListOf<GdExtTypeInfo>()

        for ((i, typeName) in typeNames.withIndex()) {
            if (i > 0 && i % 50 == 0) {
                thisLogger().info("Collecting type details: $i/${typeNames.size}...")
            }

            try {
                // Single probe doc per type - get members
                openDocument(rawServer, "extends Node\nvar _x := $typeName.new()\nfunc _r():\n\t_x.")

                val items = try {
                    val result = rawServer.textDocumentService.completion(CompletionParams(
                        TextDocumentIdentifier(probeUri), Position(3, 4)
                    )).get(3, TimeUnit.SECONDS)
                    extractItems(result) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                closeDocument(rawServer)

                val methods = mutableListOf<GdExtMethodInfo>()
                val properties = mutableListOf<GdExtPropertyInfo>()
                val signals = mutableListOf<GdExtSignalInfo>()

                for (item in items) {
                    val kind = item.kind?.value ?: continue
                    val label = item.label ?: continue
                    when (kind) {
                        LSP_KIND_METHOD -> {
                            val methodName = label.removeSuffix("(…)").removeSuffix("()")
                            methods.add(GdExtMethodInfo(methodName, emptyList(), "Variant"))
                        }
                        LSP_KIND_PROPERTY -> properties.add(GdExtPropertyInfo(label, "Variant"))
                        LSP_KIND_SIGNAL -> signals.add(GdExtSignalInfo(label))
                    }
                }

                results.add(GdExtTypeInfo(typeName, "RefCounted", methods, properties, signals))
            } catch (e: Exception) {
                thisLogger().warn("Failed to collect details for $typeName: ${e.message}")
                results.add(GdExtTypeInfo(typeName, "RefCounted", emptyList(), emptyList(), emptyList()))
            }
        }

        thisLogger().info("Collected details for ${results.size} types")
        return results
    }

    private fun openDocument(rawServer: org.eclipse.lsp4j.services.LanguageServer, text: String) {
        probeFile.writeText(text)
        rawServer.textDocumentService.didOpen(DidOpenTextDocumentParams(
            TextDocumentItem(probeUri, "gdscript", 1, text)
        ))
        Thread.sleep(100)
    }

    private fun closeDocument(rawServer: org.eclipse.lsp4j.services.LanguageServer) {
        try {
            rawServer.textDocumentService.didClose(DidCloseTextDocumentParams(
                TextDocumentIdentifier(probeUri)
            ))
        } catch (_: Exception) {}
        probeFile.delete()
    }

    private fun extractItems(result: org.eclipse.lsp4j.jsonrpc.messages.Either<List<CompletionItem>, CompletionList>?): List<CompletionItem>? {
        return when {
            result == null -> null
            result.isLeft -> result.left
            result.isRight -> result.right.items
            else -> null
        }
    }
}
