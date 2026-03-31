package gdscript.library

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import common.util.GdScriptProjectLifetimeService
import gdscript.GdScriptBundle
import kotlinx.coroutines.launch
import com.intellij.openapi.util.Version
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readText

@Service( Service.Level.PROJECT)
class GdLibraryUpdater(private val project: Project) {

    companion object {
        fun getInstance(project: Project): GdLibraryUpdater = project.getService(GdLibraryUpdater::class.java)
    }

    private val VERSION_REGEX = "config/features=PackedStringArray\\(.*\"(\\d\\.\\d)\".*\\)".toRegex()

    private fun findProjectGodot(basePath: Path, maxDepth: Int = 3): Path? {
        val direct = basePath.resolve("project.godot")
        if (direct.exists()) return direct
        if (maxDepth <= 0) return null
        basePath.toFile().listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { sub ->
            findProjectGodot(sub.toPath(), maxDepth - 1)?.let { return it }
        }
        return null
    }

    fun scheduleSkdCheck(projectBasePath: Path) {
        GdScriptProjectLifetimeService.getInstance(project).scope.launch {
            withBackgroundProgress(project, GdScriptBundle.message("progress.title.check.gdsdk.for.project")) {
                checkSdk(projectBasePath)
            }
        }
    }

    private fun checkSdk(projectBasePath: Path) {
        val projectFile = findProjectGodot(projectBasePath) ?: return
        val content = projectFile.readText(Charset.defaultCharset())

        val version = VERSION_REGEX.find(content)?.groups?.get(1)?.value
                      ?: throw IllegalStateException("GdSdk version cannot be parsed from project.godot")
        val parsed = Version.parseVersion(version)
        if (parsed == null || parsed.major < 4 || (parsed.major == 4 && parsed.minor < 6)) {
            throw IllegalStateException("Godot $version is not supported. Minimum required version is 4.6.")
        }

        // stop if disposed
        if (project.isDisposed) return

        val sdkPath = GdLibraryManager.extractSdkIfNeeded(version)
        GdLibraryManager.registerSdkIfNeeded(sdkPath, project)
    }
}