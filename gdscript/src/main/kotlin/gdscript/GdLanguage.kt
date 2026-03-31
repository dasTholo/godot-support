package gdscript

import com.intellij.lang.Language
import GdScriptBundle

object GdLanguage : Language("GdScript") {
    override fun getDisplayName(): String = GdScriptBundle.message("language.name")
}
