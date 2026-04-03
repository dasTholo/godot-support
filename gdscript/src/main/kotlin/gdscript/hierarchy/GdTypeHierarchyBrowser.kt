package gdscript.hierarchy

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.TypeHierarchyBrowserBase
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import gdscript.psi.GdClassDeclTl
import gdscript.psi.GdFile
import java.util.Comparator
import javax.swing.JPanel
import javax.swing.JTree

class GdTypeHierarchyBrowser(
    project: Project,
    private val target: PsiElement,
) : TypeHierarchyBrowserBase(project, target) {

    override fun isApplicableElement(element: PsiElement): Boolean {
        return element is GdFile || element is GdClassDeclTl
    }

    override fun createHierarchyTreeStructure(
        typeName: String,
        psiElement: PsiElement,
    ): HierarchyTreeStructure? {
        return when (typeName) {
            getSupertypesHierarchyType() -> GdSupertypeHierarchyTreeStructure(myProject, psiElement)
            getSubtypesHierarchyType() -> GdSubtypeHierarchyTreeStructure(myProject, psiElement)
            getTypeHierarchyType() -> GdTypeHierarchyTreeStructure(myProject, psiElement)
            else -> null
        }
    }

    override fun getElementFromDescriptor(descriptor: HierarchyNodeDescriptor): PsiElement? {
        return descriptor.psiElement
    }

    override fun isInterface(psiElement: PsiElement): Boolean = false

    override fun canBeDeleted(psiElement: PsiElement?): Boolean = false

    override fun getQualifiedName(psiElement: PsiElement): String {
        return when (psiElement) {
            is GdFile -> psiElement.virtualFile?.path ?: psiElement.name
            is GdClassDeclTl -> psiElement.name
            else -> psiElement.text
        }
    }

    override fun createTrees(trees: MutableMap<in String, in JTree>) {
        createTreeAndSetupCommonActions(trees, IdeActions.GROUP_TYPE_HIERARCHY_POPUP)
    }

    override fun createLegendPanel(): JPanel? = null

    override fun getComparator(): Comparator<com.intellij.ide.util.treeView.NodeDescriptor<*>>? = null

    override fun getActionPlace(): String = "TypeHierarchyPopup"

    override fun getPrevOccurenceActionNameImpl(): String = "Previous Type"

    override fun getNextOccurenceActionNameImpl(): String = "Next Type"
}
