package gdscript.extension

import com.intellij.openapi.diagnostic.thisLogger
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.Closeable
import java.net.Socket

/**
 * Minimal LSP client that communicates with the Godot editor's LSP server
 * via raw TCP socket and JSON-RPC 2.0.
 */
class GdExtensionLspClient(private val port: Int) : Closeable {

    private val gson = Gson()
    private var socket: Socket? = null
    private var requestId = 0

    fun connect(projectUri: String): Boolean {
        return try {
            val sock = Socket("127.0.0.1", port)
            sock.soTimeout = 5000
            socket = sock

            // Send initialize
            val initResult = sendRequest("initialize", mapOf(
                "processId" to null,
                "capabilities" to emptyMap<String, Any>(),
                "rootUri" to projectUri
            ))

            // Send initialized notification
            sendNotification("initialized", emptyMap<String, Any>())

            initResult != null
        } catch (e: Exception) {
            thisLogger().info("Cannot connect to Godot LSP on port $port: ${e.message}")
            false
        }
    }

    fun sendRequest(method: String, params: Any): JsonElement? {
        val sock = socket ?: return null
        val id = ++requestId
        val message = mapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "method" to method,
            "params" to params
        )
        val json = gson.toJson(message)
        val frame = "Content-Length: ${json.length}\r\n\r\n$json"
        sock.getOutputStream().write(frame.toByteArray())
        sock.getOutputStream().flush()

        // Read responses until we get the one matching our id
        while (true) {
            val response = readMessage() ?: return null
            if (response.has("id") && response.get("id").asInt == id) {
                return response.get("result")
            }
            // Skip notifications (e.g. gdscript_client/changeWorkspace, publishDiagnostics)
        }
    }

    fun sendNotification(method: String, params: Any) {
        val sock = socket ?: return
        val message = mapOf(
            "jsonrpc" to "2.0",
            "method" to method,
            "params" to params
        )
        val json = gson.toJson(message)
        val frame = "Content-Length: ${json.length}\r\n\r\n$json"
        sock.getOutputStream().write(frame.toByteArray())
        sock.getOutputStream().flush()
    }

    private fun readMessage(): JsonObject? {
        val sock = socket ?: return null
        val input = sock.getInputStream()

        // Read header
        val headerBuilder = StringBuilder()
        var prev = 0
        while (true) {
            val b = input.read()
            if (b == -1) return null
            headerBuilder.append(b.toChar())
            if (prev == '\r'.code && b == '\n'.code && headerBuilder.endsWith("\r\n\r\n")) break
            prev = b
        }

        val header = headerBuilder.toString()
        val lengthMatch = Regex("Content-Length: (\\d+)").find(header)
            ?: return null
        val length = lengthMatch.groupValues[1].toInt()

        // Read body
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n == -1) return null
            read += n
        }

        return JsonParser.parseString(String(body)).asJsonObject
    }

    override fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }
}
