package com.jetbrains.rustrover.godot.community.tscn

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.rustrover.plugins.godot.community.icons.RustRoverPluginsGodotCommunityIcons
import com.jetbrains.rustrover.godot.community.GodotCommunityBundle
import javax.swing.Icon

object TscnFileType : LanguageFileType(TscnLanguage) {
    override fun getName(): String = "Tscn file"

    override fun getDescription(): String = GodotCommunityBundle.message("filetype.tscn.file.description")

    override fun getDefaultExtension(): String = "tscn"

    override fun getIcon(): Icon = RustRoverPluginsGodotCommunityIcons.TscnFile
}
