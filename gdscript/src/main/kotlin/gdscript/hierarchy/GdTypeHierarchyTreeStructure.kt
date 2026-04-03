package gdscript.hierarchy

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import gdscript.index.impl.GdClassDeclIndex
import gdscript.index.impl.GdClassNamingIndex
import gdscript.psi.GdClassDeclTl
import gdscript.psi.GdFile
import gdscript.psi.utils.GdClassUtil
import gdscript.psi.utils.GdInheritanceUtil

// Concrete HierarchyNodeDescriptor wrapping a PsiElement
class GdHierarchyNodeDescriptor(
    project: Project,
    parent: HierarchyNodeDescriptor?,
    element: PsiElement,
    isBase: Boolean,
) : HierarchyNodeDescriptor(project, parent, element, isBase) {

    override fun update(): Boolean {
        val changes = super.update()
        val element = psiElement
        if (element != null) {
            val name = when (element) {
                is GdFile -> element.name
                is GdClassDeclTl -> element.name
                else -> element.text
            }
            myHighlightedText.ending.addText(name)
        }
        return changes
    }
}

class GdSupertypeHierarchyTreeStructure(
    project: Project,
    element: PsiElement,
) : HierarchyTreeStructure(project, GdHierarchyNodeDescriptor(project, null, element, true)) {

    override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
        val element = descriptor.psiElement ?: return emptyArray()
        val parent = GdInheritanceUtil.getExtendedElement(element, myProject) ?: return emptyArray()
        val childDescriptor = GdHierarchyNodeDescriptor(myProject, descriptor, parent, false)
        return arrayOf(childDescriptor)
    }
}

class GdSubtypeHierarchyTreeStructure(
    project: Project,
    element: PsiElement,
) : HierarchyTreeStructure(project, GdHierarchyNodeDescriptor(project, null, element, true)) {

    override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
        val element = descriptor.psiElement ?: return emptyArray()
        val className = getClassName(element) ?: return emptyArray()
        return buildChildrenForClassName(className, descriptor)
    }

    internal fun buildChildrenForClassName(className: String, descriptor: HierarchyNodeDescriptor): Array<Any> {
        val subtypes = mutableListOf<PsiElement>()

        // Check class_name declarations
        for (naming in GdClassNamingIndex.INSTANCE.getAllValues(myProject)) {
            if (naming.parentName == className) {
                subtypes.add(naming.containingFile)
            }
        }

        // Check inner class declarations
        for (classDecl in GdClassDeclIndex.INSTANCE.getAllValues(myProject)) {
            if (classDecl.parentName == className) {
                subtypes.add(classDecl)
            }
        }

        return subtypes.map { GdHierarchyNodeDescriptor(myProject, descriptor, it, false) }.toTypedArray()
    }

    private fun getClassName(element: PsiElement): String? {
        return when (element) {
            is GdFile -> GdClassUtil.getOwningClassName(element)
            is GdClassDeclTl -> element.name
            else -> null
        }
    }
}

class GdTypeHierarchyTreeStructure(
    project: Project,
    element: PsiElement,
) : HierarchyTreeStructure(project, buildRootDescriptor(project, element)) {

    companion object {
        private fun buildRootDescriptor(project: Project, element: PsiElement): HierarchyNodeDescriptor {
            var current = element
            while (true) {
                val parent = GdInheritanceUtil.getExtendedElement(current, project) ?: break
                current = parent
            }
            return GdHierarchyNodeDescriptor(project, null, current, true)
        }

        internal fun getChildrenForElement(project: Project, descriptor: HierarchyNodeDescriptor): Array<Any> {
            val element = descriptor.psiElement ?: return emptyArray()
            val className = when (element) {
                is GdFile -> GdClassUtil.getOwningClassName(element)
                is GdClassDeclTl -> element.name
                else -> return emptyArray()
            }
            val subtypes = mutableListOf<PsiElement>()
            for (naming in GdClassNamingIndex.INSTANCE.getAllValues(project)) {
                if (naming.parentName == className) {
                    subtypes.add(naming.containingFile)
                }
            }
            for (classDecl in GdClassDeclIndex.INSTANCE.getAllValues(project)) {
                if (classDecl.parentName == className) {
                    subtypes.add(classDecl)
                }
            }
            return subtypes.map { GdHierarchyNodeDescriptor(project, descriptor, it, false) }.toTypedArray()
        }
    }

    override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
        return getChildrenForElement(myProject, descriptor)
    }
}
