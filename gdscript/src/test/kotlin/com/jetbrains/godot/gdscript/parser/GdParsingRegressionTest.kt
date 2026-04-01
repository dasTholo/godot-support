package com.jetbrains.godot.gdscript.parser

import com.jetbrains.godot.getBaseTestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.pathString

@RunWith(JUnit4::class)
class GdParsingRegressionTest : GdParsingTestCase() {

    override fun getTestDataPath(): String = getBaseTestDataPath().resolve("gdscript/parser/data").pathString

    // Regression: ensure parsing completes for inline lambda with multiple semicolon-separated statements
    @Test
    fun testInlineLambdaWithSemicolons_NoInfiniteParse() {
        val code = """
            |func test():
            |	if not error_callback is Callable: error_callback = (func(str): _printerr("SimpleDungeons Error: ", str))
            |	var any_errors : = {"err": false} # So lambda closure captures
            |	error_callback = (func(str): any_errors["err"] = true; error_callback.call(str))
            |""".trimMargin()

        // Run parser synchronously (not on EDT) so the guard exception propagates directly
        val psi = createFile("sample.gd", code)
        assertNotNull(psi.firstChild)
    }
}
