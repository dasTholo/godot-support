package gdscript.extension

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.thisLogger

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
 * Queries the Godot LSP for all available types, then fetches method/property
 * details for types not already known to the SDK.
 */
class GdExtensionTypeCollector(private val client: GdExtensionLspClient) {

    companion object {
        private const val PROBE_URI = "file:///tmp/_gdext_probe.gd"
        private const val LSP_KIND_METHOD = 2
        private const val LSP_KIND_PROPERTY = 10
        private const val LSP_KIND_SIGNAL = 23 // Event kind
        private const val LSP_KIND_CLASS = 7
    }

    /**
     * Returns all type names available via LSP completion in a type-hint position.
     */
    fun collectAllTypeNames(): List<String> {
        openDocument("extends Node\nvar _x: ")

        val result = client.sendRequest("textDocument/completion", mapOf(
            "textDocument" to mapOf("uri" to PROBE_URI),
            "position" to mapOf("line" to 1, "character" to 8)
        )) ?: return emptyList()

        val items = extractCompletionItems(result)
        return items
            .filter { it.get("kind")?.asInt == LSP_KIND_CLASS }
            .map { it.get("label").asString }
    }

    /**
     * Collects full type info (methods, properties, signals) for a given type name.
     * Uses completion after an instance to get members, and hover to get method signatures.
     */
    fun collectTypeDetails(typeName: String): GdExtTypeInfo {
        // Get inheritance via hover on the type itself
        val inherits = getInheritance(typeName)

        // Open a document with an instance of the type and request completion after "._x."
        val doc = "extends Node\nvar _x := $typeName.new()\nfunc _r():\n\t_x."
        openDocument(doc)

        val result = client.sendRequest("textDocument/completion", mapOf(
            "textDocument" to mapOf("uri" to PROBE_URI),
            "position" to mapOf("line" to 3, "character" to 4)
        )) ?: return GdExtTypeInfo(typeName, inherits, emptyList(), emptyList(), emptyList())

        val items = extractCompletionItems(result)

        val methods = mutableListOf<GdExtMethodInfo>()
        val properties = mutableListOf<GdExtPropertyInfo>()
        val signals = mutableListOf<GdExtSignalInfo>()

        for (item in items) {
            val kind = item.get("kind")?.asInt ?: continue
            val label = item.get("label")?.asString ?: continue

            when (kind) {
                LSP_KIND_METHOD -> {
                    val methodName = label.removeSuffix("(…)").removeSuffix("()")
                    val sig = getMethodSignature(typeName, methodName)
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

    private fun getInheritance(typeName: String): String {
        val doc = "extends Node\nvar _x: $typeName"
        openDocument(doc)

        val result = client.sendRequest("textDocument/hover", mapOf(
            "textDocument" to mapOf("uri" to PROBE_URI),
            "position" to mapOf("line" to 1, "character" to 10)
        ))

        if (result == null || result.isJsonNull) return "RefCounted"

        val contents = result.asJsonObject.getAsJsonObject("contents")
        val value = contents?.get("value")?.asString ?: return "RefCounted"

        // Format: "\t<Native> class TypeName extends ParentType\n\n\n\n"
        val extendsMatch = Regex("extends\\s+(\\w+)").find(value)
        return extendsMatch?.groupValues?.get(1) ?: "RefCounted"
    }

    private fun getMethodSignature(typeName: String, methodName: String): GdExtMethodInfo? {
        val doc = "extends Node\nvar _x := $typeName.new()\nfunc _r():\n\t_x.$methodName()"
        openDocument(doc)

        val result = client.sendRequest("textDocument/hover", mapOf(
            "textDocument" to mapOf("uri" to PROBE_URI),
            "position" to mapOf("line" to 3, "character" to 5)
        ))

        if (result == null || result.isJsonNull) return null

        val contents = result.asJsonObject.getAsJsonObject("contents")
        val value = contents?.get("value")?.asString ?: return null

        // Format: "\tfunc TypeName.method_name(arg: Type, arg2: Type2) -> ReturnType\n\n"
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

    private fun openDocument(text: String) {
        // Close previous document
        client.sendNotification("textDocument/didClose", mapOf(
            "textDocument" to mapOf("uri" to PROBE_URI)
        ))

        // Open new document
        client.sendNotification("textDocument/didOpen", mapOf(
            "textDocument" to mapOf(
                "uri" to PROBE_URI,
                "languageId" to "gdscript",
                "version" to 1,
                "text" to text
            )
        ))

        // Small delay to let the server process
        Thread.sleep(200)
    }

    private fun extractCompletionItems(result: com.google.gson.JsonElement): List<JsonObject> {
        return when {
            result.isJsonArray -> result.asJsonArray.map { it.asJsonObject }
            result.isJsonObject -> {
                val items = result.asJsonObject.getAsJsonArray("items")
                items?.map { it.asJsonObject } ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
