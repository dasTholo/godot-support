package sdk

import java.io.File

object TemplateParser {

    fun parse(scriptTemplatesDir: File, outputDir: File) {
        if (!scriptTemplatesDir.isDirectory) return
        outputDir.mkdirs()

        for (categoryDir in scriptTemplatesDir.listFiles().orEmpty().sorted()) {
            if (!categoryDir.isDirectory) continue
            if (categoryDir.name.startsWith(".")) continue

            for (templateFile in categoryDir.listFiles().orEmpty().sorted()) {
                if (templateFile.name.startsWith(".")) continue
                if (!templateFile.isFile) continue

                val content = templateFile.readText().replace("_BASE_", "\${NAME}")
                val outputName = "${categoryDir.name} ${templateFile.name}.ft"
                outputDir.resolve(outputName).writeText(content)
            }
        }
    }
}
