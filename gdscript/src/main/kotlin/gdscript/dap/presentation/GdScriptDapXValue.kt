package gdscript.dap.presentation

import com.intellij.platform.dap.DapVariable
import com.intellij.platform.dap.xdebugger.AbstractDapXValue
import com.intellij.platform.dap.xdebugger.DapXDebuggerPresentationFactory
import com.intellij.platform.dap.DapCommandProcessor
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import javax.swing.Icon

class GdScriptDapXValue(
    factory: DapXDebuggerPresentationFactory,
    commandProcessor: DapCommandProcessor,
    variable: DapVariable,
    icon: Icon?,
    private val registry: GdScriptValuePresenterRegistry,
) : AbstractDapXValue(factory, commandProcessor, variable, icon) {

    override fun createValuePresentation(
        variable: DapVariable,
        isStructured: Boolean,
        isLazy: Boolean,
    ): XValuePresentation {
        val type = variable.type ?: return XRegularValuePresentation(variable.value, null)
        val presenter = registry.find(type)
            ?: return XRegularValuePresentation(variable.value, type)

        val formatted = presenter.formatValue(type, variable.value)
        return XRegularValuePresentation(formatted, type)
    }

}
