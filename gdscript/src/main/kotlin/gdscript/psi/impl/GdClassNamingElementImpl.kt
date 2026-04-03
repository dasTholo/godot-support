package gdscript.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import gdscript.index.stub.GdClassNamingStub
import gdscript.psi.GdClassNaming

abstract class GdClassNamingElementImpl : StubBasedPsiElementBase<GdClassNamingStub> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: GdClassNamingStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getName(): String? {
        greenStub?.let { return it.name() }
        return (this as? GdClassNaming)?.classname
    }

    override fun toString() = "GdClassNaming"

}
