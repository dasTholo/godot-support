package gdscript.parser.expr

import com.intellij.psi.tree.IElementType
import gdscript.parser.GdPsiBuilder
import gdscript.psi.GdTypes.IN
import gdscript.psi.GdTypes.IN_EX
import gdscript.psi.GdTypes.NEGATE

// comparison { "in" comparison } | comparison { "not" "in" comparison }
object GdInExParser : GdExprBaseParser() {

    override val EXPR_TYPE: IElementType = IN_EX
    override val isNested = true

    override fun parse(b: GdPsiBuilder, l: Int, optional: Boolean): Boolean {
        if (!b.recursionGuard(l, "InExpr")) return false

        // Handle "not in" compound operator
        if (b.nextTokenIs(NEGATE)) {
            if (!b.followingTokensAre(NEGATE, IN)) return false
            b.advance() // consume NEGATE
        }

        var ok = b.consumeToken(IN, pin = true)
        ok = ok && GdExprParser.parseFrom(b, l, optional, POSITION + 1)
        b.errorPin(ok, "expression")

        return ok || b.pinned()
    }

}
