package gdscript.lsp.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.columns
import gdscript.GdScriptBundle

class GodotLspSettingsConfigurable : BoundConfigurable(GdScriptBundle.message("settings.display.name")) {
    private val settings = GodotLspSettings.getInstance()

    override fun createPanel(): DialogPanel = panel {
        row(GdScriptBundle.message("settings.godot.path.label")) {
            textFieldWithBrowseButton(
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            ).bindText(settings::godotPath)
                .columns(40)
                .comment(
                    GodotLspSettings.autoDetectGodotPath()?.let {
                        GdScriptBundle.message("settings.godot.path.auto.detected", it)
                    } ?: ""
                )
        }
        row(GdScriptBundle.message("settings.lsp.port.label")) {
            intTextField(1024..65535)
                .bindIntText(settings::lspPort)
                .columns(6)
        }
    }
}
