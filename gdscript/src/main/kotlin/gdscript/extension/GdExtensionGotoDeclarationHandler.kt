package gdscript.extension

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import gdscript.psi.GdInheritanceIdRef
import gdscript.psi.GdTypeHintRef

/**
 * Redirects Ctrl+Click on GDExtension class_names from the generated .gd stub
 * to the Rust source file containing the #[derive(GodotClass)] struct.
 */
class GdExtensionGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null
        val project = sourceElement.project

        // Only handle GDScript type references and inheritance references
        val typeRef = PsiTreeUtil.getParentOfType(sourceElement, GdTypeHintRef::class.java)
            ?: PsiTreeUtil.getParentOfType(sourceElement, GdInheritanceIdRef::class.java)
        val className = typeRef?.text ?: return null

        // Check if this class_name has a Rust source
        val resolver = GdExtensionRustResolver.getInstance(project)
        val mapping = resolver.buildClassNameMapping()
        val rustLocation = mapping[className] ?: return null

        // Navigate to the struct definition in the Rust file
        val psiManager = PsiManager.getInstance(project)
        val rustPsiFile = psiManager.findFile(rustLocation.virtualFile) ?: return null

        // Try to find the exact element at the struct offset
        val elementAtOffset = rustPsiFile.findElementAt(rustLocation.offset)
        if (elementAtOffset != null) {
            return arrayOf(elementAtOffset)
        }

        return arrayOf(rustPsiFile)
    }
}
