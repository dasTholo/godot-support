package com.jetbrains.godot.gdscript.symbol

import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import gdscript.symbol.GdGotoSymbolContributor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdGotoSymbolContributorTest : BasePlatformTestCase() {

    @Test
    fun testFindsMethodsAndVariables() {
        myFixture.configureByText("Player.gd", """
            extends Node2D
            class_name Player

            var health: int = 100
            var speed: float = 200.0

            func move(delta: float) -> void:
                pass

            func take_damage(amount: int) -> void:
                pass
        """.trimIndent())

        val contributor = GdGotoSymbolContributor()
        val names = collectNames(contributor)

        assertTrue("Should find method 'move'", "move" in names)
        assertTrue("Should find method 'take_damage'", "take_damage" in names)
        assertTrue("Should find var 'health'", "health" in names)
        assertTrue("Should find var 'speed'", "speed" in names)
    }

    @Test
    fun testFindsConstsEnumsAndSignals() {
        myFixture.configureByText("Enemy.gd", """
            extends CharacterBody2D
            class_name Enemy

            const MAX_HP: int = 50
            signal died()
            enum State { IDLE, CHASE, ATTACK }

            func patrol() -> void:
                pass
        """.trimIndent())

        val contributor = GdGotoSymbolContributor()
        val names = collectNames(contributor)

        assertTrue("Should find const 'MAX_HP'", "MAX_HP" in names)
        assertTrue("Should find signal 'died'", "died" in names)
        assertTrue("Should find enum 'State'", "State" in names)
        assertTrue("Should find method 'patrol'", "patrol" in names)
    }

    @Test
    fun testProcessElementsWithName() {
        myFixture.configureByText("Weapon.gd", """
            extends Node
            class_name Weapon

            func fire() -> void:
                pass
        """.trimIndent())

        val contributor = GdGotoSymbolContributor()
        val scope = GlobalSearchScope.allScope(project)
        val elements = mutableListOf<NavigationItem>()
        contributor.processElementsWithName(
            "fire",
            Processor { elements.add(it); true },
            FindSymbolParameters.wrap("fire", scope),
        )

        assertEquals("Should find exactly one 'fire' element", 1, elements.size)
        assertEquals("fire", elements[0].name)
    }

    private fun collectNames(contributor: GdGotoSymbolContributor): Set<String> {
        val names = mutableSetOf<String>()
        contributor.processNames(
            Processor { names.add(it); true },
            GlobalSearchScope.allScope(project),
            null,
        )
        return names
    }
}
