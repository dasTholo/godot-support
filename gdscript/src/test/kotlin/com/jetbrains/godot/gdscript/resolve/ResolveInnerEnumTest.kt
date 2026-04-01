package com.jetbrains.godot.gdscript.resolve

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import gdscript.psi.GdEnumDeclTl
import gdscript.psi.GdEnumValue
import gdscript.psi.GdRefIdRef
import gdscript.reference.GdClassMemberReference

class ResolveInnerEnumTest : BasePlatformTestCase() {

    fun testResolveInnerEnumViaClassDot() {
        val code = """
            |class_name EquipmentPanel
            |
            |enum SlotPosition { NORTH, SOUTH, EAST, WEST }
            |
            |func use():
            |    var pos = EquipmentPanel.SlotPosition.NORTH
        """.trimMargin()

        val file = myFixture.configureByText("EquipmentPanel.gd", code)

        // Resolve "SlotPosition" in the chain
        val offsetSlot = file.text.indexOf("SlotPosition.NORTH")
        val elementSlot = file.findElementAt(offsetSlot)!!.parent as GdRefIdRef
        val declSlot = GdClassMemberReference(elementSlot).resolveDeclaration()
        assertNotNull("SlotPosition should resolve to inner enum", declSlot)
        assertTrue("Should resolve to GdEnumDeclTl", declSlot is GdEnumDeclTl)
        assertEquals("SlotPosition", (declSlot as GdEnumDeclTl).name)

        // Resolve "NORTH" in the chain
        val offsetNorth = file.text.indexOf("NORTH", file.text.indexOf("var pos"))
        val elementNorth = file.findElementAt(offsetNorth)!!.parent as GdRefIdRef
        val declNorth = GdClassMemberReference(elementNorth).resolveDeclaration()
        assertNotNull("NORTH should resolve to enum value", declNorth)
        assertTrue("Should resolve to GdEnumValue", declNorth is GdEnumValue)
    }

    fun testResolveInnerEnumInNestedClass() {
        val code = """
            |class A:
            |    class B:
            |        enum Direction { UP, DOWN }
            |
            |func use():
            |    var d = A.B.Direction.UP
        """.trimMargin()

        val file = myFixture.configureByText("Nested.gd", code)

        val offsetDir = file.text.indexOf("Direction.UP")
        val elementDir = file.findElementAt(offsetDir)!!.parent as GdRefIdRef
        val declDir = GdClassMemberReference(elementDir).resolveDeclaration()
        assertNotNull("Direction should resolve inside nested class", declDir)
        assertTrue(declDir is GdEnumDeclTl)

        val offsetUp = file.text.indexOf("UP", file.text.indexOf("var d"))
        val elementUp = file.findElementAt(offsetUp)!!.parent as GdRefIdRef
        val declUp = GdClassMemberReference(elementUp).resolveDeclaration()
        assertNotNull("UP should resolve to enum value", declUp)
        assertTrue(declUp is GdEnumValue)
    }
}
