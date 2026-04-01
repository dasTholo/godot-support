package project.actions

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import GdScriptPluginIcons
import project.EditorConnectionState
import gdscript.GdScriptBundle
import project.utils.GodotCommunityUtil
import project.utils.hasCompletedTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GodotActionsToolbar : DefaultActionGroup(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isVisible = false
            return
        }

        val isGodotProject = GodotCommunityUtil.isGodotProjectInstant(project)
        e.presentation.isVisible = isGodotProject


        // Determine icon/text based on connection state
        when (GodotCommunityUtil.getEditorConnectionState(project)) {
            EditorConnectionState.CONNECTED -> {
                e.presentation.icon = GdScriptPluginIcons.Icons.GodotLogo
                e.presentation.text = GdScriptBundle.message("connected.to.godot.editor.text")
            }

            EditorConnectionState.DISCONNECTED -> {
                e.presentation.icon = GdScriptPluginIcons.Icons.GodotLogo
                e.presentation.text = GdScriptBundle.message("not.connected.to.godot.editor.text")
            }

            EditorConnectionState.NOT_APPLICABLE -> {
                // No connection concept (pure GDScript) - show default connected icon
                e.presentation.icon = GdScriptPluginIcons.Icons.GodotLogo
                e.presentation.text = GdScriptBundle.message("godot.toolbar.text")
            }
        }
    }
}


@Service(Service.Level.PROJECT)
class GodotToolbarUpdateService(project: Project, scope: CoroutineScope) {
    init {
        // No reactive listener for connection state; IDE refocus polling is sufficient for Godot Editor open/close events
        scope.launch {
            // The Godot executable path does not change a lot..
            GodotCommunityUtil.getGodotExecutablePathFlow(project).collectLatest {
                ActivityTracker.getInstance().inc()
            }
        }
        scope.launch {
            GodotCommunityUtil.isGodotProject(project)?.await()
            ActivityTracker.getInstance().inc()
        }
    }

    companion object {
        fun getInstance(project: Project) = project.service<GodotToolbarUpdateService>()
    }
}
