package gdscript.extension

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.io.File

class GdExtensionStubActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (project.isDisposed) return

        val basePath = project.basePath ?: return
        val godotDir = findGodotProjectDir(File(basePath)) ?: return

        thisLogger().info("Godot project detected at $godotDir, scheduling GDExtension stub generation")

        DumbService.getInstance(project).runWhenSmart {
            if (project.isDisposed) return@runWhenSmart
            // Run on pooled thread to avoid blocking the UI
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    GdExtensionStubService.getInstance(project).generateStubs()
                } catch (e: Exception) {
                    thisLogger().info("GDExtension stub generation skipped: ${e.message}")
                }
            }
        }
    }

    private fun findGodotProjectDir(dir: File, maxDepth: Int = 3): File? {
        if (File(dir, "project.godot").exists()) return dir
        if (maxDepth <= 0) return null
        dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { sub ->
            findGodotProjectDir(sub, maxDepth - 1)?.let { return it }
        }
        return null
    }
}
