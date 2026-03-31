package project.utils

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import gdscript.GdFileType
import tscn.TscnFileType

object GodotFileUtil {

    fun isGdFile(file: VirtualFile?): Boolean = file != null && FileTypeRegistry.getInstance().isFileOfType(file, GdFileType)
    fun isTscnFile(file: VirtualFile?): Boolean = file != null && FileTypeRegistry.getInstance().isFileOfType(file, TscnFileType)

}
