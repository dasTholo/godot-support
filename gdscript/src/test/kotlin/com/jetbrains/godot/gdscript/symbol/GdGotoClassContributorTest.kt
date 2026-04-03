package com.jetbrains.godot.gdscript.symbol

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import gdscript.symbol.GdGotoClassContributor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdGotoClassContributorTest : BasePlatformTestCase() {

    @Test
    fun testFindsClassNames() {
        myFixture.configureByText("Player.gd", """
            extends Node2D
            class_name Player
        """.trimIndent())

        myFixture.configureByText("Enemy.gd", """
            extends CharacterBody2D
            class_name Enemy
        """.trimIndent())

        val contributor = GdGotoClassContributor()
        val names = collectNames(contributor)

        assertTrue("Should find 'Player'", "Player" in names)
        assertTrue("Should find 'Enemy'", "Enemy" in names)
    }

    @Test
    fun testFindsInnerClasses() {
        myFixture.configureByText("Items.gd", """
            extends Node

            class Weapon:
                var damage: int = 10

            class Armor:
                var defense: int = 5
        """.trimIndent())

        val contributor = GdGotoClassContributor()
        val names = collectNames(contributor)

        assertTrue("Should find inner class 'Weapon'", "Weapon" in names)
        assertTrue("Should find inner class 'Armor'", "Armor" in names)
    }

    @Test
    fun testClassNamingHasName() {
        myFixture.configureByText("Hero.gd", """
            extends Node2D
            class_name Hero
        """.trimIndent())

        val contributor = GdGotoClassContributor()
        val scope = GlobalSearchScope.allScope(project)
        val elements = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "Hero",
            Processor { elements.add(it); true },
            FindSymbolParameters.wrap("Hero", scope),
        )

        assertEquals(1, elements.size)
        assertEquals("Hero", elements[0].name)
    }

    private fun collectNames(contributor: GdGotoClassContributor): Set<String> {
        val names = mutableSetOf<String>()
        contributor.processNames(
            Processor { names.add(it); true },
            GlobalSearchScope.allScope(project),
            null,
        )
        return names
    }
}
