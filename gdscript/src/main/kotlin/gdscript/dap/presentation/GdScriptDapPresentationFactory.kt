package gdscript.dap.presentation

import com.intellij.platform.dap.DapCommandProcessor
import com.intellij.platform.dap.DapVariable
import com.intellij.platform.dap.xdebugger.DefaultDapXDebuggerPresentationFactory
import com.intellij.xdebugger.frame.XNamedValue
import gdscript.dap.presentation.presenters.*
import javax.swing.Icon

class GdScriptDapPresentationFactory : DefaultDapXDebuggerPresentationFactory() {

    private val registry = GdScriptValuePresenterRegistry(
        listOf(
            ColorPresenter(),
            VectorPresenter(),
            TransformPresenter(),
            NodePresenter(),
            CollectionPresenter(),
            DefaultPresenter(),
        )
    )

    override fun createValue(
        commandProcessor: DapCommandProcessor,
        variable: DapVariable,
        icon: Icon?,
    ): XNamedValue {
        return GdScriptDapXValue(this, commandProcessor, variable, icon, registry)
    }
}
