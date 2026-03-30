package com.jetbrains.rustrover.godot.community.utils

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rustrover.godot.community.gdscript.GdFileType
import com.jetbrains.rustrover.godot.community.tscn.TscnFileType

object GodotFileUtil {

    fun isGdFile(file: VirtualFile?): Boolean = file != null && FileTypeRegistry.getInstance().isFileOfType(file, GdFileType)
    fun isTscnFile(file: VirtualFile?): Boolean = file != null && FileTypeRegistry.getInstance().isFileOfType(file, TscnFileType)

}
