package sdk

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object ClassParser {

    fun formatType(type: String): String {
        if (type.endsWith("[]")) {
            return "Array[${type.dropLast(2)}]"
        }
        return type
    }

    fun parse(xmlDirs: List<File>, outputDir: File) {
        val dbf = DocumentBuilderFactory.newInstance()
        outputDir.mkdirs()

        val allFiles = mutableListOf<File>()
        for (xmlDir in xmlDirs) {
            if (!xmlDir.isDirectory) continue
            allFiles.addAll(xmlDir.listFiles().orEmpty().filter { it.name.endsWith(".xml") })
        }

        for (xmlFile in allFiles.sortedBy { it.name }) {
            val doc = dbf.newDocumentBuilder().parse(xmlFile)
            val root = doc.documentElement
            val att = root.attributes

            val rawName = att.getNamedItem("name")?.textContent
                ?: xmlFile.nameWithoutExtension
            val className = rawName.replace("@", "_")
            val inherits = att.getNamedItem("inherits")?.textContent
            val isDeprecated = att.getNamedItem("is_deprecated")?.textContent
            val isExperimental = att.getNamedItem("is_experimental")?.textContent

            val sb = StringBuilder()

            // Class header
            if (inherits != null) {
                sb.appendLine("extends $inherits")
            }
            sb.appendLine("class_name $className")
            sb.appendLine()

            // Documentation
            val brief = getChildText(root, "brief_description")
            val description = getChildText(root, "description")
            val tutorials = parseTutorials(root)
            addDocumentation(sb, description, brief, tutorials, isDeprecated, isExperimental)
            sb.appendLine()
            sb.appendLine()

            // Signals
            forEachChild(root, "signals", "signal") { signal ->
                val signalAtt = signal.attributes
                val signalName = signalAtt.getNamedItem("name").textContent
                val desc = getChildText(signal, "description")
                val sigDeprecated = signalAtt.getNamedItem("is_deprecated")?.textContent
                val sigExperimental = signalAtt.getNamedItem("is_experimental")?.textContent
                addDocumentation(sb, desc, null, null, sigDeprecated, sigExperimental)

                val params = parseParams(signal)
                val paramsStr = if (params.isNotEmpty()) "(${params.joinToString(", ")})" else ""
                sb.appendLine("signal $signalName$paramsStr")
            }

            // Constants and enums
            val enums = linkedMapOf<String, MutableList<String>>()
            forEachChild(root, "constants", "constant") { constant ->
                val cAtt = constant.attributes
                val cName = cAtt.getNamedItem("name").textContent
                val cValue = cAtt.getNamedItem("value").textContent
                val cEnum = cAtt.getNamedItem("enum")?.textContent
                val cDeprecated = cAtt.getNamedItem("is_deprecated")?.textContent
                val cExperimental = cAtt.getNamedItem("is_experimental")?.textContent
                val cDesc = constant.textContent?.trim()

                addDocumentation(sb, cDesc, null, null, cDeprecated, cExperimental)

                if (cEnum != null) {
                    enums.getOrPut(cEnum) { mutableListOf() }.add("$cName = $cValue,\n")
                } else {
                    sb.appendLine("const $cName = $cValue;")
                    sb.appendLine()
                }
            }
            sb.appendLine()

            // Unnamed enum blocks
            for ((enumName, values) in enums) {
                sb.appendLine("#enum $enumName")
                sb.appendLine("enum {")
                for (v in values) {
                    sb.append("    $v")
                }
                sb.appendLine("}")
            }

            // Named enum blocks (skip enums with dots — nested enums)
            for ((enumName, values) in enums) {
                if ('.' in enumName) continue
                sb.appendLine("#enum $enumName")
                sb.appendLine("enum $enumName {")
                for (v in values) {
                    sb.append("    $v")
                }
                sb.appendLine("}")
            }

            // Members (skip ProjectSettings)
            val getSetMethods = StringBuilder()
            if (xmlFile.nameWithoutExtension != "ProjectSettings") {
                forEachChild(root, "members", "member") { member ->
                    val mAtt = member.attributes
                    val mName = mAtt.getNamedItem("name").textContent
                    val mType = formatType(mAtt.getNamedItem("type").textContent)
                    val getter = mAtt.getNamedItem("getter")?.textContent
                    val setter = mAtt.getNamedItem("setter")?.textContent
                    val mDeprecated = mAtt.getNamedItem("is_deprecated")?.textContent
                    val mExperimental = mAtt.getNamedItem("is_experimental")?.textContent
                    val mDesc = member.textContent?.trim()

                    addDocumentation(sb, mDesc, null, null, mDeprecated, mExperimental)
                    sb.append("var $mName: $mType")

                    val getSet = mutableListOf<String>()
                    if (!getter.isNullOrEmpty()) {
                        getSet.add("get = $getter")
                        getSetMethods.appendLine("func $getter() -> $mType:")
                        getSetMethods.appendLine("\treturn $mName")
                        getSetMethods.appendLine()
                    }
                    if (!setter.isNullOrEmpty()) {
                        getSet.add("set = $setter")
                        getSetMethods.appendLine("func $setter(value: $mType) -> void:")
                        getSetMethods.appendLine("\t$mName = value")
                        getSetMethods.appendLine()
                    }
                    if (getSet.isNotEmpty()) {
                        sb.appendLine(":")
                        sb.append("\t${getSet.joinToString(", ")}")
                    }
                    sb.appendLine()
                    sb.appendLine()
                }
                sb.appendLine()
            }

            // Constructors
            forEachChild(root, "constructors", "constructor") { ctor ->
                val cAtt = ctor.attributes
                val cName = cAtt.getNamedItem("name").textContent
                val retEl = getFirstChild(ctor, "return")
                val retType = formatType(retEl?.getAttribute("type") ?: "void")
                val desc = getChildText(ctor, "description")
                val cDeprecated = cAtt.getNamedItem("is_deprecated")?.textContent
                val cExperimental = cAtt.getNamedItem("is_experimental")?.textContent

                addDocumentation(sb, desc, null, null, cDeprecated, cExperimental)

                val params = parseParams(ctor)
                sb.appendLine("func $cName(${params.joinToString(", ")}) -> $retType:")
                sb.appendLine("\tpass;")
                sb.appendLine()
            }
            sb.appendLine()

            // Methods
            forEachChild(root, "methods", "method") { method ->
                val mAtt = method.attributes
                val mName = mAtt.getNamedItem("name").textContent
                val retEl = getFirstChild(method, "return")
                val retType = formatType(retEl?.getAttribute("type") ?: "void")
                val qualifiers = mAtt.getNamedItem("qualifiers")?.textContent ?: ""
                val desc = getChildText(method, "description")
                val mDeprecated = mAtt.getNamedItem("is_deprecated")?.textContent
                val mExperimental = mAtt.getNamedItem("is_experimental")?.textContent

                val allowed = setOf("static", "vararg")
                val quali = qualifiers.split(" ")
                    .filter { it in allowed }
                    .joinToString(" ")
                val qualiPrefix = if (quali.isNotEmpty()) "$quali " else ""

                addDocumentation(sb, desc, null, null, mDeprecated, mExperimental)

                val params = parseParams(method)
                sb.appendLine("${qualiPrefix}func $mName(${params.joinToString(", ")}) -> $retType:")
                sb.appendLine("\tpass;")
                sb.appendLine()
            }
            sb.appendLine()
            sb.append(getSetMethods)

            outputDir.resolve("$className.gd").writeText(sb.toString())
        }
    }

    // --- Private helpers ---

    private fun addDocumentation(
        sb: StringBuilder,
        description: String?,
        brief: String?,
        tutorials: List<Pair<String?, String>>?,
        deprecated: String?,
        experimental: String?
    ) {
        if (brief.isNullOrEmpty() && description.isNullOrEmpty()
            && tutorials.isNullOrEmpty() && deprecated != "true" && experimental != "true") {
            return
        }

        var addSpace = false
        if (!brief.isNullOrEmpty()) {
            addSpace = true
            for (line in brief.lines()) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    sb.appendLine("## $trimmed")
                }
            }
        }
        if (!description.isNullOrEmpty()) {
            if (addSpace) sb.appendLine("##")
            addSpace = true
            for (line in description.lines()) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    sb.appendLine("## $trimmed")
                }
            }
        }
        if (!tutorials.isNullOrEmpty()) {
            if (addSpace) sb.appendLine("##")
            for ((title, url) in tutorials) {
                val titlePart = if (!title.isNullOrEmpty()) "($title)" else ""
                sb.appendLine("## @tutorial$titlePart: $url")
            }
        }
        if (deprecated == "true") {
            sb.appendLine("## @deprecated")
        }
        if (experimental == "true") {
            sb.appendLine("## @experimental")
        }
    }

    private val baseTutorialUrl = "https://docs.godotengine.org/en/stable"

    private fun parseTutorials(root: Element): List<Pair<String?, String>> {
        val tutorials = mutableListOf<Pair<String?, String>>()
        val tutorialsNode = getFirstChild(root, "tutorials") ?: return tutorials
        val links = tutorialsNode.getElementsByTagName("link")
        for (i in 0 until links.length) {
            val link = links.item(i) as Element
            val title = link.getAttribute("title").ifEmpty { null }
            val url = link.textContent.trim().replace("\$DOCS_URL", baseTutorialUrl)
            tutorials.add(title to url)
        }
        return tutorials
    }

    private fun parseParams(parent: Element): List<String> {
        val paramNodes = parent.getElementsByTagName("param")
        val indexed = sortedMapOf<Int, Triple<String, String, String?>>()

        for (i in 0 until paramNodes.length) {
            val param = paramNodes.item(i) as Element
            // Only direct children (skip nested params from description etc.)
            if (param.parentNode != parent) continue

            val index = param.getAttribute("index").toIntOrNull() ?: i
            var pName = param.getAttribute("name")
            if (pName == "var") pName = "variable"
            val pType = param.getAttribute("type")
            val pDefault = if (param.hasAttribute("default")) param.getAttribute("default") else null
            indexed[index] = Triple(pName, pType, pDefault)
        }

        return indexed.values.map { (name, type, default) ->
            val formattedType = formatType(type)
            if (default != null) "$name: $formattedType = $default" else "$name: $formattedType"
        }
    }

    private fun getChildText(parent: Element, tagName: String): String? {
        val nodes = parent.getElementsByTagName(tagName)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as Element
            if (node.parentNode == parent) {
                return node.textContent?.trim()?.ifEmpty { null }
            }
        }
        return null
    }

    private fun getFirstChild(parent: Element, tagName: String): Element? {
        val nodes = parent.getElementsByTagName(tagName)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as Element
            if (node.parentNode == parent) return node
        }
        return null
    }

    private fun forEachChild(root: Element, wrapperTag: String, itemTag: String, action: (Element) -> Unit) {
        val wrapper = getFirstChild(root, wrapperTag) ?: return
        val items = wrapper.getElementsByTagName(itemTag)
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            if (item.parentNode == wrapper) {
                action(item)
            }
        }
    }
}
