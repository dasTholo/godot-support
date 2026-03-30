package godot.lsp.service

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import kotlin.io.path.Path

class GodotLsp4jClient(
    handler: LspServerNotificationsHandler,
    private val project: Project
) : Lsp4jClient(handler) {

    private class GodotChangeWorkspaceParams(
        val path: String
    )

    @JsonNotification("gdscript_client/changeWorkspace")
    private fun changeWorkspace(params: GodotChangeWorkspaceParams) {
        thisLogger().info("Received gdscript_client/changeWorkspace with path: ${params.path}")

        val projectBasePath = project.basePath ?: return
        val workspacePath = Path(params.path)
        val expectedPath = Path(projectBasePath)

        if (workspacePath != expectedPath) {
            thisLogger().warn(
                "Workspace path $workspacePath doesn't match project path $expectedPath, disconnecting"
            )
            GodotLspNotification.getInstance(project).showNonMatchingProjectWarning()
            LspServerManager.getInstance(project).stopServers(GodotLspServerSupportProvider::class.java)
        }
    }
}
