package godot.lsp.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.columns
import godot.lsp.GodotLspBundle

class GodotLspSettingsConfigurable : BoundConfigurable(GodotLspBundle.message("settings.display.name")) {
    private val settings = GodotLspSettings.getInstance()

    override fun createPanel(): DialogPanel = panel {
        row(GodotLspBundle.message("settings.godot.path.label")) {
            textFieldWithBrowseButton(
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            ).bindText(settings::godotPath)
                .columns(40)
                .comment(
                    GodotLspSettings.autoDetectGodotPath()?.let {
                        GodotLspBundle.message("settings.godot.path.auto.detected", it)
                    } ?: ""
                )
        }
        row(GodotLspBundle.message("settings.lsp.port.label")) {
            intTextField(1024..65535)
                .bindIntText(settings::lspPort)
                .columns(6)
        }
    }
}
