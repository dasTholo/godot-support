package gdscript.dap.presentation.presenters

import gdscript.dap.presentation.GdScriptValuePresenter
import javax.swing.Icon

class TransformPresenter : GdScriptValuePresenter {

    private val supportedTypes = setOf("Transform2D", "Transform3D", "Basis")

    private val axisNames = mapOf(
        "Transform2D" to listOf("x", "y", "origin"),
        "Transform3D" to listOf("x", "y", "z", "origin"),
        "Basis" to listOf("x", "y", "z"),
    )

    override fun canPresent(type: String): Boolean = type in supportedTypes

    override fun formatValue(type: String, value: String): String {
        val names = axisNames[type] ?: return "$type: $value"
        val components = parseComponents(value)
        if (components.size != names.size) return "$type: $value"

        val labeled = names.zip(components).joinToString(", ") { (name, comp) -> "$name: $comp" }
        return "$type($labeled)"
    }

    override fun getIcon(type: String, value: String): Icon? = null

    companion object {
        private val COMPONENT_PATTERN = Regex("""\([^()]+\)""")

        fun parseComponents(value: String): List<String> =
            COMPONENT_PATTERN.findAll(value).map { it.value }.toList()
    }
}
