package gdscript.dap.evaluation

import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import gdscript.GdLanguage
import gdscript.completion.GdLookup
import gdscript.completion.utils.GdMethodCompletionUtil.lookup
import gdscript.index.impl.GdClassNamingIndex
import gdscript.psi.utils.GdClassMemberUtil
import gdscript.psi.utils.GdClassMemberUtil.methods
import gdscript.psi.utils.GdClassUtil
import gdscript.psi.GdClassVarDeclTl
import gdscript.psi.GdConstDeclTl

/**
 * Provides autocompletion in Watch / Evaluate Expression windows during debug sessions.
 *
 * When the user types e.g. "Vector2." in the evaluate dialog, this contributor resolves
 * the class name before the dot and offers its members (methods, variables, constants).
 */
class GdScriptDebugCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(GdLanguage),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    val project = parameters.position.project
                    val text = parameters.position.containingFile?.text ?: return

                    val offset = parameters.offset
                    val beforeCursor = text.substring(0, offset)
                    val dotIndex = beforeCursor.lastIndexOf('.')
                    if (dotIndex < 0) return

                    val prefix = beforeCursor.substring(0, dotIndex).trim()
                    if (prefix.isEmpty()) return

                    // Try to resolve the prefix as a known class name (e.g. "Vector2", "Node2D")
                    val classNaming = GdClassNamingIndex.INSTANCE.getGlobally(prefix, project)
                        .firstOrNull() ?: return
                    val classElement = GdClassUtil.getOwningClassElement(classNaming)

                    // Collect all members including inherited ones
                    val members = mutableListOf<Any>()
                    GdClassMemberUtil.collectFromParents(classElement, members, project)

                    // Add methods
                    for (method in members.methods()) {
                        result.addElement(method.lookup())
                    }

                    // Add variables
                    for (variable in members.filterIsInstance<GdClassVarDeclTl>()) {
                        result.addElement(
                            GdLookup.create(
                                variable.name,
                                typed = variable.returnType,
                                priority = GdLookup.USER_DEFINED,
                            )
                        )
                    }

                    // Add constants
                    for (constant in members.filterIsInstance<GdConstDeclTl>()) {
                        result.addElement(
                            GdLookup.create(
                                constant.name,
                                typed = constant.returnType,
                                priority = GdLookup.USER_DEFINED,
                            )
                        )
                    }
                }
            }
        )
    }
}
