package godot.lsp

import com.intellij.lang.Language

object GdLanguage : Language("GdScript") {
    override fun getDisplayName(): String = "GDScript"
}
