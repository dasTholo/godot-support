package gdscript.parser.expr

import com.intellij.psi.tree.IElementType
import gdscript.parser.GdPsiBuilder
import gdscript.parser.common.GdTypedParser
import gdscript.psi.GdTypes.IS
import gdscript.psi.GdTypes.IS_EX
import gdscript.psi.GdTypes.NEGATE

// is = call [ "is" [ "not" ] ( IDENTIFIER | BUILTINTYPE ) ] ;
object GdIsExParser : GdExprBaseParser() {

    override val EXPR_TYPE: IElementType = IS_EX
    override val isNested = true

    override fun parse(b: GdPsiBuilder, l: Int, optional: Boolean): Boolean {
        if (!b.recursionGuard(l, "IsExpr")) return false
        var ok = b.consumeToken(IS, pin = true)

        // Handle "is not" compound operator
        if (b.nextTokenIs(NEGATE)) {
            b.advance() // consume NEGATE
        }

        ok = ok && GdTypedParser.typedVal(b, l + 1, false)
        b.errorPin(ok, "type")

        return ok || b.pinned()
    }

}
