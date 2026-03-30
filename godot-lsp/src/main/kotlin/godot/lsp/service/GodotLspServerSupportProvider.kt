package godot.lsp.service

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspCommunicationChannel
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCompletionCustomizer
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspDiagnosticsCustomizer
import com.intellij.platform.lsp.api.customization.LspDiagnosticsSupport
import com.intellij.platform.lsp.api.customization.LspInlayHintCustomizer
import com.intellij.platform.lsp.api.customization.LspInlayHintDisabled
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import godot.lsp.GodotLspIcons
import godot.lsp.GodotUtil
import godot.lsp.settings.GodotLspSettings
import godot.lsp.settings.GodotLspSettingsConfigurable

class GodotLspServerSupportProvider : LspServerSupportProvider {

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (!GodotUtil.isGdFile(file)) return

        val basePath = project.basePath ?: return
        val godotDir = GodotUtil.findGodotProjectDir(file, basePath)
            ?: if (GodotUtil.hasGodotProjectFile(basePath)) java.io.File(basePath) else null
        if (godotDir == null) return

        thisLogger().info("Godot project detected at ${godotDir.path}, starting LSP for ${file.name}")
        serverStarter.ensureServerStarted(GodotLspServerDescriptor(project))
    }

    override fun createLspServerWidgetItem(
        lspServer: LspServer,
        currentFile: VirtualFile?
    ): LspServerWidgetItem = GodotLspWidgetItem(
        lspServer,
        currentFile,
        GodotLspIcons.GodotLogo,
        settingsPageClass = GodotLspSettingsConfigurable::class.java
    )

    private class GodotLspServerDescriptor(project: Project) :
        ProjectWideLspServerDescriptor(project, "Godot") {

        private val settings = GodotLspSettings.getInstance()
        private val port get() = settings.lspPort

        override fun isSupportedFile(file: VirtualFile): Boolean = GodotUtil.isGdFile(file)

        override val lspCommunicationChannel: LspCommunicationChannel
            get() {
                thisLogger().info("Connecting to Godot LSP on port $port")
                return LspCommunicationChannel.Socket(port, false)
            }

        override fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient {
            return GodotLsp4jClient(handler, project)
        }

        override val lspCustomization: LspCustomization = object : LspCustomization() {
            // Handle "$" prefix for node path completion (RIDER-119006)
            override val completionCustomizer: LspCompletionCustomizer = object : LspCompletionSupport() {
                override fun getCompletionPrefix(
                    parameters: CompletionParameters,
                    defaultPrefix: String
                ): String =
                    if (defaultPrefix.startsWith("$")) defaultPrefix.substringAfter("$")
                    else defaultPrefix
            }

            override val inlayHintCustomizer: LspInlayHintCustomizer = LspInlayHintDisabled

            override val diagnosticsCustomizer: LspDiagnosticsCustomizer = object : LspDiagnosticsSupport() {
                override fun getHighlightSeverity(
                    diagnostic: org.eclipse.lsp4j.Diagnostic
                ): com.intellij.lang.annotation.HighlightSeverity? {
                    // Filter out noisy diagnostics
                    if (diagnostic.message.startsWith("(UNUSED_PARAMETER)")) return null
                    if (diagnostic.message.startsWith("Class") && diagnostic.message.contains("defined in global scope")) return null
                    return super.getHighlightSeverity(diagnostic)
                }
            }
        }
    }
}
