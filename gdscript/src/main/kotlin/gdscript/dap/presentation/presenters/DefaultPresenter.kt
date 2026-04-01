package gdscript.dap.presentation.presenters

import gdscript.dap.presentation.GdScriptValuePresenter
import javax.swing.Icon

class DefaultPresenter : GdScriptValuePresenter {
    override fun canPresent(type: String): Boolean = true
    override fun formatValue(type: String, value: String): String = value
    override fun getIcon(type: String, value: String): Icon? = null
}
