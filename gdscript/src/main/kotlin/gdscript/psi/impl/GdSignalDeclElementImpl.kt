package gdscript.psi.impl

import GdScriptPluginIcons
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.stubs.IStubElementType
import javax.swing.Icon
import gdscript.index.stub.GdSignalDeclStub

abstract class GdSignalDeclElementImpl : StubBasedPsiElementBase<GdSignalDeclStub> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: GdSignalDeclStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? = name
            override fun getLocationString(): String? = containingFile?.name
            override fun getIcon(unused: Boolean): Icon? = GdScriptPluginIcons.GDScriptIcons.SIGNAL_MARKER
        }
    }

    override fun getIcon(flags: Int): Icon = GdScriptPluginIcons.GDScriptIcons.SIGNAL_MARKER

    override fun toString() = "GdSignalDecl"

}
