package gdscript.lsp

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

    fun findGodotProjectDir(file: VirtualFile, projectBasePath: String): java.io.File? {
        var dir = file.parent
        while (dir != null && dir.path.startsWith(projectBasePath)) {
            if (dir.findChild(PROJECT_FILE) != null) {
                return java.io.File(dir.path)
            }
            dir = dir.parent
        }
        return null
    }
}
