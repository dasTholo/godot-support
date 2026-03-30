package gdscript.structureView

import com.intellij.ide.projectView.ProjectViewNestingRulesProvider

class GdFileNesting : ProjectViewNestingRulesProvider {

    private val importExts = arrayOf("svg", "png", "jpg", "jpeg")
    private val uidExts = arrayOf("gd", "tscn", "tres", "gdshader", "gdshaderinc")

    override fun addFileNestingRules(consumer: ProjectViewNestingRulesProvider.Consumer) {
        importExts.forEach { consumer.addNestingRule(".$it", ".$it.import") }
        uidExts.forEach { consumer.addNestingRule(".$it", ".$it.uid") }
    }
}
