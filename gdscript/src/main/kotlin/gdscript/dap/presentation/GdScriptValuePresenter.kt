package gdscript.dap.presentation

import javax.swing.Icon

interface GdScriptValuePresenter {
    fun canPresent(type: String): Boolean
    fun formatValue(type: String, value: String): String
    // TODO: Custom icons deferred — AbstractDapXValue.getDefaultIcon() is final.
    //  Wire when platform API provides a hook (e.g., via icon parameter in createValue).
    fun getIcon(type: String, value: String): Icon?
}
