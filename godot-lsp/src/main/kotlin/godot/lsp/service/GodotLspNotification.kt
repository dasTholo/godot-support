package godot.lsp.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import godot.lsp.GodotLspBundle

@Service(Service.Level.PROJECT)
class GodotLspNotification(private val project: Project) {

    companion object {
        private const val GROUP_ID = "Godot LSP"

        fun getInstance(project: Project): GodotLspNotification = project.service<GodotLspNotification>()
    }

    private var activeNotification: Notification? = null

    fun showNonMatchingProjectWarning() {
        activeNotification?.expire()

        val notification = Notification(
            GROUP_ID,
            GodotLspBundle.message("notification.title.godot.lsp.warning"),
            GodotLspBundle.message("notification.content.lsp.attempted.to.connect.to.non.matching.project"),
            NotificationType.WARNING
        )

        activeNotification = notification
        Notifications.Bus.notify(notification, project)
    }
}
