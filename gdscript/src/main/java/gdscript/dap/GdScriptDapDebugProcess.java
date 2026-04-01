package gdscript.dap;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.platform.dap.DapDebugSession;
import com.intellij.platform.dap.DapStartRequest;
import com.intellij.platform.dap.DebugAdapterDescriptor;
import com.intellij.platform.dap.xdebugger.DapXDebugProcess;
import com.intellij.platform.dap.xdebugger.DapXDebuggerPresentationFactory;
import com.intellij.xdebugger.XDebugSession;
import gdscript.dap.presentation.GdScriptDapPresentationFactory;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

// Java required: getPresentationFactory() is non-open in Kotlin metadata,
// but overridable at the JVM level. Java ignores Kotlin's open/final metadata.
public class GdScriptDapDebugProcess extends DapXDebugProcess {

    private final GdScriptDapPresentationFactory gdScriptPresentationFactory =
            new GdScriptDapPresentationFactory();

    public GdScriptDapDebugProcess(
            @NotNull XDebugSession session,
            @NotNull DapDebugSession dapDebugSession,
            @NotNull CoroutineScope xDebugProcessScope,
            @NotNull CoroutineScope globalScope,
            @NotNull DebugAdapterDescriptor<?> debugAdapterDescriptor,
            @NotNull ExecutionEnvironment executionEnvironment,
            @Nullable ExecutionResult executionResult,
            @NotNull DapStartRequest startRequestType,
            @NotNull Map<String, ?> startRequestArguments
    ) {
        super(session, dapDebugSession, xDebugProcessScope, globalScope,
                debugAdapterDescriptor, executionEnvironment, executionResult,
                startRequestType, startRequestArguments);
    }

    @NotNull
    @Override
    protected DapXDebuggerPresentationFactory getPresentationFactory() {
        return gdScriptPresentationFactory;
    }
}
