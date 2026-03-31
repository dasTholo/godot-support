package gdscript.lsp.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "GodotLspSettings", storages = [Storage("GodotLspSettings.xml")])
class GodotLspSettings : PersistentStateComponent<GodotLspSettings> {
    var godotPath: String = ""
    var lspPort: Int = 6005

    override fun getState(): GodotLspSettings = this
    override fun loadState(state: GodotLspSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getEffectiveGodotPath(): String {
        return godotPath.ifBlank { autoDetectGodotPath() ?: "" }
    }

    companion object {
        fun getInstance(): GodotLspSettings =
            ApplicationManager.getApplication().getService(GodotLspSettings::class.java)

        fun autoDetectGodotPath(): String? {
            val candidates = listOf(
                "godot4", "godot",
                "/usr/bin/godot4", "/usr/bin/godot",
                "/usr/local/bin/godot4", "/usr/local/bin/godot",
                "/snap/bin/godot4",
            )
            // Check PATH first
            for (name in candidates.take(2)) {
                try {
                    val process = ProcessBuilder("which", name).start()
                    val path = process.inputStream.bufferedReader().readText().trim()
                    if (process.waitFor() == 0 && path.isNotBlank()) return path
                } catch (_: Exception) { }
            }
            // Check absolute paths
            for (path in candidates.drop(2)) {
                if (java.io.File(path).canExecute()) return path
            }
            // Check Flatpak
            try {
                val process = ProcessBuilder("flatpak", "list", "--columns=application").start()
                val output = process.inputStream.bufferedReader().readText()
                if (process.waitFor() == 0 && output.contains("org.godotengine.Godot")) {
                    return "flatpak run org.godotengine.Godot"
                }
            } catch (_: Exception) { }
            return null
        }
    }
}
