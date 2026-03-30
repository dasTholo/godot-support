package gdscript.extension

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class GdExtensionStubActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (project.isDisposed) return

        val basePath = project.basePath ?: return
        val projectGodot = java.io.File(basePath, "project.godot")
        if (!projectGodot.exists()) return

        thisLogger().info("Godot project detected, scheduling GDExtension stub generation")

        try {
            GdExtensionStubService.getInstance(project).generateStubs()
        } catch (e: Exception) {
            thisLogger().info("GDExtension stub generation skipped: ${e.message}")
        }
    }
}
