package gdscript.extension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

class GdExtensionRefreshAction : AnAction("Refresh GDExtension Types") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, "Refreshing GDExtension types...", true
        ) {
            override fun run(indicator: ProgressIndicator) {
                GdExtensionStubService.getInstance(project).generateStubs()
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
