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

        // Matches #[func] or #[func(rename=...)] fn name(params) -> ReturnType
        private val FUNC_FULL_PATTERN = Regex(
            """#\[func(?:\(([^)]*)\))?\]\s*(?:pub\s+)?fn\s+(\w+)\s*\(([^)]*)\)(?:\s*->\s*([^\s{]+(?:<[^>]+>)?))?""",
            RegexOption.MULTILINE
        )

        private val BASE_CLASS_PATTERN = Regex(
            """#\[class\([^)]*base\s*=\s*(\w+)""",
            RegexOption.MULTILINE
        )

        private val RUST_TO_GDSCRIPT = mapOf(
            "GString" to "String",
            "StringName" to "String",
            "bool" to "bool",
            "f32" to "float",
            "f64" to "float",
            "i8" to "int",
            "i16" to "int",
            "i32" to "int",
            "i64" to "int",
            "u8" to "int",
            "u16" to "int",
            "u32" to "int",
            "u64" to "int",
            "Variant" to "Variant",
            "VarDictionary" to "Dictionary",
            "PackedByteArray" to "PackedByteArray",
            "PackedInt32Array" to "PackedInt32Array",
            "PackedInt64Array" to "PackedInt64Array",
            "PackedFloat32Array" to "PackedFloat32Array",
            "PackedFloat64Array" to "PackedFloat64Array",
            "PackedStringArray" to "PackedStringArray",
            "PackedVector2Array" to "PackedVector2Array",
            "PackedVector3Array" to "PackedVector3Array",
            "PackedColorArray" to "PackedColorArray",
            "PackedVector4Array" to "PackedVector4Array",
            "Vector2" to "Vector2",
            "Vector2i" to "Vector2i",
            "Vector3" to "Vector3",
            "Vector3i" to "Vector3i",
            "Vector4" to "Vector4",
            "Vector4i" to "Vector4i",
            "Color" to "Color",
            "Rect2" to "Rect2",
            "Rect2i" to "Rect2i",
            "Transform2D" to "Transform2D",
            "Transform3D" to "Transform3D",
            "Basis" to "Basis",
            "Quaternion" to "Quaternion",
            "AABB" to "AABB",
            "Plane" to "Plane",
            "Projection" to "Projection",
            "RID" to "RID",
            "Callable" to "Callable",
            "Signal" to "Signal",
            "NodePath" to "NodePath",
        )

        fun mapRustType(rustType: String, className: String): String {
            RUST_TO_GDSCRIPT[rustType]?.let { return it }

            val gdMatch = Regex("""Gd<(\w+)>""").find(rustType)
            if (gdMatch != null) {
                val inner = gdMatch.groupValues[1]
                return if (inner == "Self") className else inner
            }

            val optionGdMatch = Regex("""Option<Gd<(\w+)>>""").find(rustType)
            if (optionGdMatch != null) {
                val inner = optionGdMatch.groupValues[1]
                return if (inner == "Self") className else inner
            }

            val arrayGdMatch = Regex("""Array<Gd<(\w+)>>""").find(rustType)
            if (arrayGdMatch != null) {
                val inner = arrayGdMatch.groupValues[1]
                return "Array[${if (inner == "Self") className else inner}]"
            }

            if (rustType.startsWith("Array<")) return "Array"
            if (rustType.startsWith("Dictionary<")) return "Dictionary"

            return "Variant"
        }

        private fun splitRustParams(params: String): List<String> {
            val result = mutableListOf<String>()
            var depth = 0
            var start = 0
            for (i in params.indices) {
                when (params[i]) {
                    '<' -> depth++
                    '>' -> depth--
                    ',' -> if (depth == 0) {
                        result.add(params.substring(start, i))
                        start = i + 1
                    }
                }
            }
            if (start < params.length) result.add(params.substring(start))
            return result
        }
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

    fun collectMethodSignatures(className: String): List<RustMethodInfo> {
        val mapping = buildClassNameMapping()
        val location = mapping[className] ?: return emptyList()

        val content = try {
            String(location.virtualFile.contentsToByteArray(), Charsets.UTF_8)
        } catch (_: Exception) {
            return emptyList()
        }

        val methods = mutableListOf<RustMethodInfo>()
        FUNC_FULL_PATTERN.findAll(content).forEach { match ->
            val funcAttr = match.groupValues[1]
            val methodName = match.groupValues[2]
            val paramsStr = match.groupValues[3].trim()
            val returnStr = match.groupValues[4].trim()

            val renameMatch = RENAME_PATTERN.find(funcAttr)
            val gdMethodName = renameMatch?.groupValues?.get(1) ?: methodName

            val params = mutableListOf<RustMethodParam>()
            var isStatic = true
            if (paramsStr.isNotEmpty()) {
                val paramParts = splitRustParams(paramsStr)
                for (part in paramParts) {
                    val trimmed = part.trim()
                    if (trimmed == "&self" || trimmed == "&mut self" || trimmed == "self") {
                        isStatic = false
                        continue
                    }
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx > 0) {
                        val paramName = trimmed.substring(0, colonIdx).trim()
                        val paramType = trimmed.substring(colonIdx + 1).trim()
                        params.add(RustMethodParam(paramName, mapRustType(paramType, className)))
                    }
                }
            }

            val gdReturnType = if (returnStr.isEmpty()) "void" else mapRustType(returnStr, className)
            methods.add(RustMethodInfo(gdMethodName, params, gdReturnType, isStatic))
        }

        return methods
    }

    fun getBaseClass(className: String): String? {
        val mapping = buildClassNameMapping()
        val location = mapping[className] ?: return null

        val content = try {
            String(location.virtualFile.contentsToByteArray(), Charsets.UTF_8)
        } catch (_: Exception) {
            return null
        }

        return BASE_CLASS_PATTERN.find(content)?.groupValues?.get(1)
    }

    override fun dispose() {}
}
