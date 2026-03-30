package godot.lsp

import com.intellij.openapi.vfs.VirtualFile

object GodotUtil {
    const val GD_EXTENSION = "gd"
    const val PROJECT_FILE = "project.godot"

    fun isGdFile(file: VirtualFile?): Boolean =
        file?.extension?.equals(GD_EXTENSION, ignoreCase = true) ?: false

    fun hasGodotProjectFile(basePath: String): Boolean {
        val projectFile = java.io.File(basePath, PROJECT_FILE)
        return projectFile.exists()
    }
}
