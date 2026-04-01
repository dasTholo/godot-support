package gdscript.dap.presentation

import com.intellij.platform.dap.DapCommandProcessor
import com.intellij.platform.dap.DapScope
import com.intellij.platform.dap.xdebugger.DefaultDapXDebuggerPresentationFactory
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueGroup

class GdScriptDapScope(
    private val factory: DefaultDapXDebuggerPresentationFactory,
    private val commandProcessor: DapCommandProcessor,
    private val scope: DapScope,
    private val index: Int,
) : XValueGroup(scope.name) {

    override fun isAutoExpand(): Boolean = index == 0

    override fun computeChildren(node: XCompositeNode) {
        // Delegate to the default scope behavior for now.
        // The scope variables are loaded by the platform and then
        // our PresentationFactory.createValue() handles each variable's presentation.
        // This class exists as the extension point for future Node Children grouping.
        val defaultScope = factory.createScope(commandProcessor, scope, index)
        defaultScope.computeChildren(node)
    }
}
