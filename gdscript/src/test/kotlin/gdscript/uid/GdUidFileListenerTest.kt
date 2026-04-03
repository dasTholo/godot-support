package gdscript.uid

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.application.runWriteAction
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GdUidFileListenerTest : BasePlatformTestCase() {

    @Test
    fun `test uid file created for new gd file in godot project`() {
        // Create project.godot to mark as Godot project
        runWriteAction {
            myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "project.godot")
        }

        // Create a .gd file
        val gdFile = runWriteAction {
            myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "test_script.gd")
        }
        runWriteAction {
            VfsUtil.saveText(gdFile, "extends Node\nclass_name TestScript\n")
        }

        // Trigger the listener manually
        val projectDir = myFixture.tempDirFixture.findOrCreateDir("")
        GdUidFileListener.generateUidIfNeeded(gdFile, projectDir)

        // Verify .uid file was created
        val uidFile = projectDir.findChild("test_script.gd.uid")
        assertNotNull("UID file should be created", uidFile)

        val content = VfsUtil.loadText(uidFile!!)
        assertTrue("UID content should start with uid://", content.startsWith("uid://"))
    }

    @Test
    fun `test uid file not created when already exists`() {
        runWriteAction {
            myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "project.godot")
        }

        // Pre-create .uid file BEFORE creating the .gd file
        // (the registered listener would otherwise auto-create it)
        val uidFile = runWriteAction {
            val f = myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "existing.gd.uid")
            VfsUtil.saveText(f, "uid://alreadyexists")
            f
        }

        val gdFile = runWriteAction {
            myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "existing.gd")
        }

        val projectDir = myFixture.tempDirFixture.findOrCreateDir("")
        GdUidFileListener.generateUidIfNeeded(gdFile, projectDir)

        // Verify content unchanged
        assertEquals("uid://alreadyexists", VfsUtil.loadText(uidFile))
    }

    @Test
    fun `test uid file not created for non-godot project`() {
        // No project.godot file
        val gdFile = runWriteAction {
            myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "no_project.gd")
        }

        // projectDir without project.godot — should not create uid
        val projectDir = myFixture.tempDirFixture.findOrCreateDir("")
        GdUidFileListener.generateUidIfNeeded(gdFile, projectDir)

        val uidFile = projectDir.findChild("no_project.gd.uid")
        assertNull("UID file should NOT be created without project.godot", uidFile)
    }

    @Test
    fun `test uid file created for gdshader file`() {
        runWriteAction {
            myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "project.godot")
        }

        val shaderFile = runWriteAction {
            myFixture.tempDirFixture.findOrCreateDir("").createChildData(this, "test.gdshader")
        }

        val projectDir = myFixture.tempDirFixture.findOrCreateDir("")
        GdUidFileListener.generateUidIfNeeded(shaderFile, projectDir)

        val uidFile = projectDir.findChild("test.gdshader.uid")
        assertNotNull("UID file should be created for .gdshader", uidFile)
    }
}
