package sdk

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object SdkBuilder {

    private val tagRegex = Regex("""^.*?refs/tags/(.+?)-stable$""")

    fun parseStableTags(lines: List<String>, minMinor: Int = 6): List<String> {
        return lines.mapNotNull { line ->
            tagRegex.matchEntire(line)?.groupValues?.get(1)
        }.filter { tag ->
            if (!tag.startsWith("4.")) return@filter false
            val minor = tag.removePrefix("4.").split(".").firstOrNull()?.toIntOrNull() ?: return@filter false
            minor >= minMinor
        }
    }

    fun build(sdkDir: File) {
        sdkDir.mkdirs()
        val tempDir = createTempDir("godot-sdk-build")

        try {
            // Fetch tags
            val tagLines = fetchTags()
            val tags = parseStableTags(tagLines)
            println("Found ${tags.size} tags to process: $tags")

            // Process each tag + Master
            val allVersions = tags + "Master"
            for (version in allVersions) {
                println("Processing $version...")
                val versionDir = sdkDir.resolve(version).also { it.mkdirs() }
                val sourceDir = downloadAndExtract(version, tempDir)
                runParsers(sourceDir, versionDir)
                sourceDir.deleteRecursively()
            }

            // Package as tar.xz
            val archiveFile = sdkDir.resolve("sdk.tar.xz")
            packageSdk(sdkDir, archiveFile)
            println("SDK built: ${archiveFile.absolutePath}")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun fetchTags(): List<String> {
        val process = ProcessBuilder("git", "ls-remote", "--tags", "https://github.com/godotengine/godot")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readLines()
        val exitCode = process.waitFor()
        if (exitCode != 0) error("git ls-remote failed with exit code $exitCode")
        return output
    }

    private fun downloadAndExtract(version: String, tempDir: File): File {
        val downloadTag = if (version == "Master") "master" else "${version.lowercase()}-stable"
        val tarGzFile = tempDir.resolve("$downloadTag.tar.gz")

        // Download
        val url = "https://github.com/godotengine/godot/archive/$downloadTag.tar.gz"
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        client.send(request, HttpResponse.BodyHandlers.ofFile(tarGzFile.toPath()))

        // Extract
        val extractDir = tempDir.resolve("extract-$version")
        extractDir.mkdirs()
        ProcessBuilder("tar", "-xf", tarGzFile.absolutePath, "-C", extractDir.absolutePath)
            .redirectErrorStream(true)
            .start()
            .waitFor()

        tarGzFile.delete()

        // Find the extracted directory (godot-master or godot-4.6-stable etc.)
        return extractDir.listFiles()?.firstOrNull { it.isDirectory }
            ?: error("No directory found after extracting $downloadTag")
    }

    fun collectXmlDirs(godotSourceDir: File): List<File> {
        val dirs = mutableListOf<File>()

        val docClasses = godotSourceDir.resolve("doc/classes")
        if (docClasses.isDirectory) dirs.add(docClasses)

        val modulesDir = godotSourceDir.resolve("modules")
        if (modulesDir.isDirectory) {
            for (module in modulesDir.listFiles().orEmpty().sorted()) {
                if (!module.isDirectory || module.name.startsWith(".")) continue
                val docDir = module.resolve("doc_classes")
                if (docDir.isDirectory) dirs.add(docDir)
            }
        }

        return dirs
    }

    private fun runParsers(godotSourceDir: File, outputDir: File) {
        val xmlDirs = collectXmlDirs(godotSourceDir)

        ClassParser.parse(xmlDirs, outputDir)
        OperandParser.parse(xmlDirs, outputDir.resolve("operators.gdconf"))
        // AnnotationParser: find @GDScript.xml in modules/gdscript/doc_classes/
        val gdscriptXml = godotSourceDir.resolve("modules/gdscript/doc_classes/@GDScript.xml")
        if (gdscriptXml.exists()) {
            AnnotationParser.parse(gdscriptXml, outputDir.resolve("annotation.gdconf"))
        }
    }

    private fun packageSdk(sdkDir: File, archiveFile: File) {
        FileOutputStream(archiveFile).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                XZCompressorOutputStream(bos).use { xzOut ->
                    TarArchiveOutputStream(xzOut).use { tar ->
                        tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)

                        for (versionDir in sdkDir.listFiles().orEmpty().sorted()) {
                            if (!versionDir.isDirectory) continue
                            addDirectoryToTar(tar, versionDir, versionDir.name)
                        }

                        tar.finish()
                    }
                }
            }
        }
    }

    private fun addDirectoryToTar(tar: TarArchiveOutputStream, dir: File, basePath: String) {
        for (file in dir.listFiles().orEmpty().sorted()) {
            val entryName = "$basePath/${file.name}"
            if (file.isDirectory) {
                addDirectoryToTar(tar, file, entryName)
            } else {
                val entry = TarArchiveEntry(file, entryName)
                tar.putArchiveEntry(entry)
                file.inputStream().use { it.copyTo(tar) }
                tar.closeArchiveEntry()
            }
        }
    }
}
