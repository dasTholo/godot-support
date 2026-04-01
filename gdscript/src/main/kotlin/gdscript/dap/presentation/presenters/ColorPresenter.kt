package gdscript.dap.presentation.presenters

import gdscript.dap.presentation.GdScriptValuePresenter
import java.awt.Color
import javax.swing.Icon

class ColorPresenter : GdScriptValuePresenter {

    override fun canPresent(type: String): Boolean = type == "Color"

    override fun formatValue(type: String, value: String): String {
        val inner = if (value.startsWith("Color(")) value else "Color$value"
        return inner
    }

    override fun getIcon(type: String, value: String): Icon? {
        val color = parseColor(value) ?: return null
        return ColorSwatchIcon(color, 12)
    }

    companion object {
        private val RGBA_PATTERN = Regex("""\(?\s*([0-9.]+)\s*,\s*([0-9.]+)\s*,\s*([0-9.]+)\s*,\s*([0-9.]+)\s*\)?""")

        fun parseColor(value: String): Color? {
            val stripped = value.removePrefix("Color")
            val match = RGBA_PATTERN.find(stripped) ?: return null
            return try {
                val r = match.groupValues[1].toFloat().coerceIn(0f, 1f)
                val g = match.groupValues[2].toFloat().coerceIn(0f, 1f)
                val b = match.groupValues[3].toFloat().coerceIn(0f, 1f)
                val a = match.groupValues[4].toFloat().coerceIn(0f, 1f)
                Color(r, g, b, a)
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}

private class ColorSwatchIcon(private val color: Color, private val size: Int) : Icon {
    override fun paintIcon(c: java.awt.Component?, g: java.awt.Graphics, x: Int, y: Int) {
        val g2 = g.create() as java.awt.Graphics2D
        try {
            g2.color = color
            g2.fillRect(x + 1, y + 1, size - 2, size - 2)
            g2.color = Color.GRAY
            g2.drawRect(x, y, size - 1, size - 1)
        } finally {
            g2.dispose()
        }
    }

    override fun getIconWidth(): Int = size
    override fun getIconHeight(): Int = size
}
