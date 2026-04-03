package gdscript.hierarchy

import com.intellij.ide.hierarchy.HierarchyBrowser
import com.intellij.ide.hierarchy.HierarchyProvider
import com.intellij.ide.hierarchy.TypeHierarchyBrowserBase
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import gdscript.psi.GdClassDeclTl
import gdscript.psi.GdClassNaming
import gdscript.psi.GdFile

class GdTypeHierarchyProvider : HierarchyProvider {

    override fun getTarget(dataContext: DataContext): PsiElement? {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return null
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
        return getTarget(editor, file)
    }

    fun getTarget(editor: Editor, file: PsiFile): PsiElement? {
        if (file !is GdFile) return null
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return file

        val classDecl = PsiTreeUtil.getParentOfType(element, GdClassDeclTl::class.java)
        if (classDecl != null) return classDecl

        val classNaming = PsiTreeUtil.getParentOfType(element, GdClassNaming::class.java)
        if (classNaming != null) return file

        return file
    }

    override fun createHierarchyBrowser(target: PsiElement): HierarchyBrowser {
        return GdTypeHierarchyBrowser(target.project, target)
    }

    override fun browserActivated(hierarchyBrowser: HierarchyBrowser) {
        (hierarchyBrowser as GdTypeHierarchyBrowser).changeView(TypeHierarchyBrowserBase.getTypeHierarchyType())
    }
}
