package gdscript.dap.presentation.presenters

import gdscript.dap.presentation.GdScriptValuePresenter
import javax.swing.Icon

class CollectionPresenter : GdScriptValuePresenter {

    override fun canPresent(type: String): Boolean = type == "Array" || type == "Dictionary"

    override fun formatValue(type: String, value: String): String {
        return when (type) {
            "Array" -> formatArray(value)
            "Dictionary" -> formatDictionary(value)
            else -> "$type: $value"
        }
    }

    override fun getIcon(type: String, value: String): Icon? = null

    private fun formatArray(value: String): String {
        if (!value.startsWith("[")) return "Array: $value"
        val inner = value.removePrefix("[").removeSuffix("]").trim()
        if (inner.isEmpty()) return "Array (empty)"
        val count = countTopLevelElements(inner)
        return "Array ($count elements)"
    }

    private fun formatDictionary(value: String): String {
        if (!value.startsWith("{")) return "Dictionary: $value"
        val inner = value.removePrefix("{").removeSuffix("}").trim()
        if (inner.isEmpty()) return "Dictionary (empty)"
        val count = countTopLevelElements(inner)
        return "Dictionary ($count entries)"
    }

    companion object {
        fun countTopLevelElements(inner: String): Int {
            var depth = 0
            var count = 1
            for (ch in inner) {
                when (ch) {
                    '[', '{', '(' -> depth++
                    ']', '}', ')' -> depth--
                    ',' -> if (depth == 0) count++
                }
            }
            return count
        }
    }
}
