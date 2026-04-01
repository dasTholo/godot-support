package gdscript.dap.presentation.presenters

import gdscript.dap.presentation.GdScriptValuePresenter
import javax.swing.Icon

class VectorPresenter : GdScriptValuePresenter {

    private val supportedTypes = setOf(
        "Vector2", "Vector2i", "Vector3", "Vector3i", "Vector4", "Vector4i",
        "Rect2", "Rect2i", "AABB", "Plane", "Quaternion"
    )

    override fun canPresent(type: String): Boolean = type in supportedTypes

    override fun formatValue(type: String, value: String): String {
        if (value.startsWith(type)) return value
        return "$type$value"
    }

    override fun getIcon(type: String, value: String): Icon? = null
}
