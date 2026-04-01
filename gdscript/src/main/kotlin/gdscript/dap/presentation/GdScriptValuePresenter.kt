package gdscript.dap.presentation

import javax.swing.Icon

interface GdScriptValuePresenter {
    fun canPresent(type: String): Boolean
    fun formatValue(type: String, value: String): String
    fun getIcon(type: String, value: String): Icon?
}
