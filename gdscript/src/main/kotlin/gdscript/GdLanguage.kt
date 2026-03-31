package gdscript

import com.intellij.lang.Language
import gdscript.GdScriptBundle

object GdLanguage : Language("GdScript") {
    override fun getDisplayName(): String = GdScriptBundle.message("language.name")
}
