package gdscript.utils

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.project.Project
import project.utils.GodotCommunityUtil
import java.nio.file.Path
import java.nio.file.Paths

private const val RUSTROVER_GODOT_PLUGIN_ID = "com.intellij.rustrover.godot"
fun PluginManagerCore.isRustRoverGodotSupportPluginInstalled(): Boolean {
    return this.plugins.any { it.pluginId.idString == RUSTROVER_GODOT_PLUGIN_ID && it.isEnabled }
}

// todo: prop init may be delayed, we may need to somehow postpone index building
fun Project.getMainProjectBasePath(): Path? {
    return GodotCommunityUtil.getGodotProjectBasePath(this)
        ?: this.basePath?.let { Paths.get(it) }
}
