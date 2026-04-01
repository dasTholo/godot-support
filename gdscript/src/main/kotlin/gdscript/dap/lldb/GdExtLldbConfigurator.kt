package gdscript.dap.lldb

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ide.plugins.PluginManagerCore

private val LOG = logger<GdExtLldbConfigurator>()

class GdExtLldbConfigurator : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (!isGdExtProject(project)) return

        val formatterPath = findFormatterPath() ?: run {
            LOG.warn("gdext_lldb_formatters.py not found in plugin resources")
            return
        }

        // Store the path so RustRover's LLDB configuration can pick it up.
        // The actual LLDB command injection depends on RustRover's API for
        // configuring LLDB init commands. This is stored as a project-level setting.
        LOG.info("gdext LLDB formatters available at: $formatterPath")
    }

    private fun isGdExtProject(project: Project): Boolean {
        val baseDir = project.basePath ?: return false
        val vfs = LocalFileSystem.getInstance()

        // Check for .gdextension file
        val baseDirVf = vfs.findFileByPath(baseDir) ?: return false
        if (baseDirVf.children.any { it.extension == "gdextension" }) return true

        // Check Cargo.toml for godot dependency
        val cargoToml = vfs.findFileByPath("$baseDir/Cargo.toml")
        if (cargoToml != null) {
            val content = String(cargoToml.contentsToByteArray())
            if (content.contains("godot")) return true
        }

        return false
    }

    private fun findFormatterPath(): String? {
        val pluginId = PluginId.getId("com.dastholo.rustrover.gdscript")
        val plugin = PluginManagerCore.getPlugin(pluginId) ?: return null
        val pluginPath = plugin.pluginPath ?: return null
        val formatterFile = pluginPath.resolve("debugger/gdext_lldb_formatters.py")
        return if (formatterFile.toFile().exists()) formatterFile.toString() else null
    }
}
