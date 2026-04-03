package gdscript.uid

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class GdUidFileListener : BulkFileListener {

    companion object {
        private val UID_EXTENSIONS = setOf("gd", "gdshader", "gdextension")

        /**
         * Generate a .uid file for [file] if it doesn't exist yet and [projectDir]
         * contains a project.godot (i.e., is a Godot project).
         *
         * Exposed as companion for testability.
         */
        fun generateUidIfNeeded(file: VirtualFile, projectDir: VirtualFile) {
            if (!isGodotProject(projectDir)) return

            val parent = file.parent ?: return
            val uidFileName = "${file.name}.uid"

            if (parent.findChild(uidFileName) != null) return

            val existingUids = collectExistingUids(projectDir)
            val uid = GdUidGenerator.generateUniqueUid(existingUids)

            runWriteAction {
                val uidFile = parent.createChildData(GdUidFileListener::class.java, uidFileName)
                VfsUtil.saveText(uidFile, uid)
            }

            thisLogger().info("Generated UID file: ${parent.path}/$uidFileName")
        }

        private fun isGodotProject(dir: VirtualFile): Boolean {
            var current: VirtualFile? = dir
            while (current != null) {
                if (current.findChild("project.godot") != null) return true
                current = current.parent
            }
            return false
        }

        private fun collectExistingUids(projectDir: VirtualFile): Set<String> {
            val uids = mutableSetOf<String>()
            VfsUtil.iterateChildrenRecursively(projectDir, { vf ->
                // Skip .godot directory
                !(vf.isDirectory && vf.name == ".godot")
            }) { vf ->
                if (!vf.isDirectory && vf.name.endsWith(".uid")) {
                    try {
                        uids.add(VfsUtil.loadText(vf).trim())
                    } catch (_: Exception) {}
                }
                true
            }
            return uids
        }

        private fun findGodotProjectRoot(file: VirtualFile): VirtualFile? {
            var current: VirtualFile? = file.parent
            while (current != null) {
                if (current.findChild("project.godot") != null) return current
                current = current.parent
            }
            return null
        }
    }

    override fun after(events: List<VFileEvent>) {
        for (event in events) {
            val file = event.file ?: continue
            if (file.extension !in UID_EXTENSIONS) continue
            if (file.name.endsWith(".uid")) continue

            val projectRoot = findGodotProjectRoot(file) ?: continue
            generateUidIfNeeded(file, projectRoot)
        }
    }
}
