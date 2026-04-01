package gdscript.settings

import GdProjectService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import gdscript.GdScriptBundle
import java.nio.file.Path
import javax.swing.JComponent

class GdSettingsConfigurable(val project: Project) : Configurable {

    private var component: GdSettingsComponent? = null


    override fun getDisplayName(): String {
        return GdScriptBundle.message("settings.configurable.name.gdscript.settings")
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return component?.preferredFocusedComponent()
    }

    override fun createComponent(): JComponent? {
        component = GdSettingsComponent(project)

        return component?.panel
    }

    override fun isModified(): Boolean {
        val settings = GdProjectSettingsState.getInstance(project).state

        return component?.hidePrivate != settings.hidePrivate
            || component?.shortTyped != settings.shortTyped
            || component?.annotators != settings.annotators
            || component?.criticals != settings.criticals
            || component?.warnings != settings.warnings
            || component?.notes != settings.notes
            || component?.docProvider != settings.docProvider
            || component?.lspConnectionMode?.name != settings.lspConnectionMode
            || component?.lspRemoteHostPort != settings.lspRemoteHostPort
            || component?.lspUseDynamicPort != settings.lspUseDynamicPort
            || component?.godotProjectPath != settings.godotProjectPath
    }

    override fun apply() {
        val settings = GdProjectSettingsState.getInstance(project).state
        settings.hidePrivate = component?.hidePrivate ?: true
        settings.shortTyped = component?.shortTyped ?: false
        settings.annotators = component?.annotators ?: GdProjectState.OFF
        settings.criticals = component?.criticals ?: "ALERT,ATTENTION,CAUTION,CRITICAL,DANGER,SECURITY"
        settings.warnings = component?.warnings ?: "BUG,DEPRECATED,FIXME,HACK,TASK,TBD,TODO,WARNING"
        settings.notes = component?.notes ?: "INFO,NOTE,NOTICE,TEST,TESTING"
        settings.docProvider = component?.docProvider ?: GdDocProviderMode.GDSCRIPT
        settings.lspConnectionMode = (component?.lspConnectionMode ?: GdLspConnectionMode.ConnectRunningEditor).name
        settings.lspRemoteHostPort = component?.lspRemoteHostPort ?: 6005
        settings.lspUseDynamicPort = component?.lspUseDynamicPort ?: false
        settings.godotProjectPath = component?.godotProjectPath ?: ""

        GdLspSettingsFlowService.getInstance(project).settingsChanged()

        // Re-discover Godot project if path changed
        val newPath = settings.godotProjectPath
        if (newPath.isNotBlank()) {
            val manualDir = VfsUtil.findFile(Path.of(newPath), true)
            if (manualDir != null) {
                GdProjectService.getInstance(project).discoverProject(manualDir)
            }
        } else {
            // Reset to auto-detection: re-init will happen on next project open
            GdProjectService.getInstance(project).autoDetectProject()
        }
    }

    override fun reset() {
        val settings = GdProjectSettingsState.getInstance(project).state
        component?.hidePrivate = settings.hidePrivate
        component?.shortTyped = settings.shortTyped
        component?.annotators = settings.annotators
        component?.criticals = settings.criticals
        component?.warnings = settings.warnings
        component?.notes = settings.notes
        component?.docProvider = settings.docProvider
        component?.lspConnectionMode = GdLspConnectionMode.valueOf(settings.lspConnectionMode)
        component?.lspRemoteHostPort = settings.lspRemoteHostPort
        component?.lspUseDynamicPort = settings.lspUseDynamicPort
        component?.godotProjectPath = settings.godotProjectPath
    }

    override fun disposeUIResources() {
        component = null
    }

}
