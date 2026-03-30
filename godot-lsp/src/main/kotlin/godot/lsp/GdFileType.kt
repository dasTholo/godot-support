package godot.lsp

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object GdFileType : LanguageFileType(GdLanguage) {
    override fun getName(): String = "GDScript"
    override fun getDescription(): String = "GDScript file"
    override fun getDefaultExtension(): String = "gd"
    override fun getIcon(): Icon = GodotLspIcons.GodotLogo
}
