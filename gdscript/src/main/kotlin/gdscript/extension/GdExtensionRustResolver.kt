package gdscript.extension

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class GdExtensionRustResolver(private val project: Project) : Disposable {

    data class RustClassLocation(val virtualFile: VirtualFile, val structName: String, val offset: Int)

    private val cache = AtomicReference<Map<String, RustClassLocation>?>(null)

    init {
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    if (events.any { it.file?.extension == "rs" }) {
                        cache.set(null)
                    }
                }
            }
        )
    }

    companion object {
        fun getInstance(project: Project): GdExtensionRustResolver =
            project.getService(GdExtensionRustResolver::class.java)

        private val GODOT_CLASS_PATTERN = Regex(
            """#\[derive\([^)]*GodotClass[^)]*\)\]\s*(?:#\[class\(([^)]*)\)\]\s*)?(?:pub\s+)?struct\s+(\w+)""",
            RegexOption.MULTILINE
        )

        private val RENAME_PATTERN = Regex("""rename\s*=\s*(\w+)""")

        // Matches #[func] fn method_name(params) — captures method name and parameter list
        private val FUNC_PATTERN = Regex(
            """#\[func\]\s*(?:pub\s+)?fn\s+(\w+)\s*\(([^)]*)\)""",
            RegexOption.MULTILINE
        )
    }

    fun buildClassNameMapping(): Map<String, RustClassLocation> {
        cache.get()?.let { return it }

        val result = mutableMapOf<String, RustClassLocation>()
        val basePath = project.basePath ?: return result
        val baseDir = VfsUtil.findFile(File(basePath).toPath(), true) ?: return result

        VfsUtil.collectChildrenRecursively(baseDir)
            .filter { it.extension == "rs" && !it.path.contains("/target/") }
            .forEach { vf -> parseRustFile(vf, result) }

        cache.set(result)
        return result
    }

    private fun parseRustFile(vf: VirtualFile, result: MutableMap<String, RustClassLocation>) {
        val content = try {
            String(vf.contentsToByteArray(), Charsets.UTF_8)
        } catch (_: Exception) {
            return
        }

        GODOT_CLASS_PATTERN.findAll(content).forEach { match ->
            val classAttr = match.groupValues[1]
            val structName = match.groupValues[2]
            val offset = match.range.first

            val renameMatch = RENAME_PATTERN.find(classAttr)
            val className = renameMatch?.groupValues?.get(1) ?: structName

            result[className] = RustClassLocation(vf, structName, offset)
        }
    }

    /**
     * Collect static method names for a given GDExtension class.
     * A method is static if it has #[func] and its first parameter is NOT &self or &mut self.
     * In godot-rust, this means it's callable as ClassName.method() from GDScript.
     */
    fun collectStaticMethods(className: String): Set<String> {
        val mapping = buildClassNameMapping()
        val location = mapping[className] ?: return emptySet()

        val content = try {
            String(location.virtualFile.contentsToByteArray(), Charsets.UTF_8)
        } catch (_: Exception) {
            return emptySet()
        }

        val staticMethods = mutableSetOf<String>()
        FUNC_PATTERN.findAll(content).forEach { match ->
            val methodName = match.groupValues[1]
            val params = match.groupValues[2].trim()
            // Static if first param is NOT &self or &mut self
            if (!params.startsWith("&self") && !params.startsWith("&mut self")) {
                staticMethods.add(methodName)
            }
        }

        return staticMethods
    }

    override fun dispose() {}
}
