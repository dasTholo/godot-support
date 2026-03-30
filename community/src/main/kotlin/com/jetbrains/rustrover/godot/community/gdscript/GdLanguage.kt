package com.jetbrains.rustrover.godot.community.gdscript

import com.intellij.lang.Language
import com.jetbrains.rustrover.godot.community.GodotCommunityBundle

object GdLanguage : Language("GdScript") {
    override fun getDisplayName(): String = GodotCommunityBundle.message("language.name")
}
