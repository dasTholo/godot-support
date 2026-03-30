package tscn.psi

import com.intellij.psi.tree.IElementType
import com.jetbrains.rustrover.godot.community.tscn.TscnLanguage

class TscnElementType : IElementType {

    constructor(debugName: String) : super(debugName, TscnLanguage)

}
