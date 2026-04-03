package gdscript.psi.impl

import GdScriptPluginIcons
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.stubs.IStubElementType
import javax.swing.Icon
import gdscript.index.stub.GdClassDeclStub

abstract class GdClassDeclElementImpl : StubBasedPsiElementBase<GdClassDeclStub> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: GdClassDeclStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? = name
            override fun getLocationString(): String? = containingFile?.name
            override fun getIcon(unused: Boolean): Icon? = GdScriptPluginIcons.GDScriptIcons.OBJECT
        }
    }

    override fun getIcon(flags: Int): Icon = GdScriptPluginIcons.GDScriptIcons.OBJECT

    override fun toString() = "GdClassDecl"

}
