package tscn.psi

import com.intellij.psi.tree.IElementType
import com.jetbrains.rustrover.godot.community.tscn.TscnLanguage

class TscnTokenType : IElementType {

    constructor(debugName: String) : super(debugName, TscnLanguage)

    override fun toString(): String = "TscnTokenType.${super.toString()}"

}