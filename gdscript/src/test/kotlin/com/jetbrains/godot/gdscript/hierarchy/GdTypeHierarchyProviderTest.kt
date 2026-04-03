package com.jetbrains.godot.gdscript.hierarchy

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import gdscript.hierarchy.GdSupertypeHierarchyTreeStructure
import gdscript.hierarchy.GdSubtypeHierarchyTreeStructure
import gdscript.hierarchy.GdTypeHierarchyProvider
import gdscript.psi.GdClassNaming
import gdscript.psi.GdFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdTypeHierarchyProviderTest : BasePlatformTestCase() {

    @Test
    fun testProviderFindsClassAtCaret() {
        val file = myFixture.configureByText("Player.gd", """
            extends Node2D
            class_name Pla<caret>yer
        """.trimIndent())

        val provider = GdTypeHierarchyProvider()
        val target = provider.getTarget(myFixture.editor, file)

        assertNotNull("Provider should find target at caret", target)
    }

    @Test
    fun testSupertypeHierarchy() {
        myFixture.configureByText("Base.gd", """
            extends RefCounted
            class_name Base
        """.trimIndent())

        val childFile = myFixture.configureByText("Child.gd", """
            extends Base
            class_name Child
        """.trimIndent())

        val classNaming = PsiTreeUtil.findChildOfType(childFile, GdClassNaming::class.java)!!
        val structure = GdSupertypeHierarchyTreeStructure(project, classNaming.containingFile)

        val root = structure.rootElement as HierarchyNodeDescriptor
        val children = structure.getChildElements(root)

        assertTrue("Should have at least one supertype (Base)", children.isNotEmpty())
        val firstChild = children[0] as HierarchyNodeDescriptor
        val parentElement = firstChild.psiElement
        assertNotNull(parentElement)
        assertTrue(
            "Supertype should be Base file",
            parentElement is GdFile && parentElement.name == "Base.gd",
        )
    }

    @Test
    fun testSubtypeHierarchy() {
        val parentFile = myFixture.configureByText("Animal.gd", """
            extends Node
            class_name Animal
        """.trimIndent())

        myFixture.configureByText("Dog.gd", """
            extends Animal
            class_name Dog
        """.trimIndent())

        myFixture.configureByText("Cat.gd", """
            extends Animal
            class_name Cat
        """.trimIndent())

        val structure = GdSubtypeHierarchyTreeStructure(project, parentFile)

        val root = structure.rootElement as HierarchyNodeDescriptor
        val children = structure.getChildElements(root)

        val childNames = children.mapNotNull {
            ((it as HierarchyNodeDescriptor).psiElement as? GdFile)?.name
        }.toSet()

        assertTrue("Should find Dog.gd as subtype", "Dog.gd" in childNames)
        assertTrue("Should find Cat.gd as subtype", "Cat.gd" in childNames)
    }
}
