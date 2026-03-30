package godot.lsp.service

import com.intellij.lang.LangBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerState
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import godot.lsp.GodotLspBundle
import javax.swing.Icon

class GodotLspWidgetItem(
    lspServer: LspServer,
    currentFile: VirtualFile?,
    icon: Icon,
    settingsPageClass: Class<out Configurable>? = null
) : LspServerWidgetItem(lspServer, currentFile, icon, settingsPageClass) {

    override val widgetActionText: String
        get() = when (lspServer.state) {
            LspServerState.Initializing ->
                LangBundle.message("language.services.widget.item.initializing", serverLabel)
            LspServerState.Running -> serverLabel
            LspServerState.ShutdownNormally ->
                LangBundle.message("language.services.widget.item.shutdown.normally", serverLabel)
            LspServerState.ShutdownUnexpectedly ->
                GodotLspBundle.message("language.services.widget.item.shutdown.unexpectedly", serverLabel)
        }
}
