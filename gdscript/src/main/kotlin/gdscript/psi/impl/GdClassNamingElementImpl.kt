package gdscript.psi.impl

import GdScriptPluginIcons
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.stubs.IStubElementType
import javax.swing.Icon
import gdscript.index.stub.GdClassNamingStub
import gdscript.psi.GdClassNaming

abstract class GdClassNamingElementImpl : StubBasedPsiElementBase<GdClassNamingStub> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: GdClassNamingStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getName(): String? {
        greenStub?.let { return it.name() }
        return (this as? GdClassNaming)?.classname
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? = name
            override fun getLocationString(): String? = containingFile?.name
            override fun getIcon(unused: Boolean): Icon? = GdScriptPluginIcons.GDScriptIcons.OBJECT
        }
    }

    override fun getIcon(flags: Int): Icon = GdScriptPluginIcons.GDScriptIcons.OBJECT

    override fun toString() = "GdClassNaming"

}
