package gdscript.dap.scenetree

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.treeStructure.Tree
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.tree.DefaultMutableTreeNode

class GdScriptSceneTreeTab : Disposable {

    val model = GdScriptSceneTreeModel()
    private val tree = Tree(model)
    val component: JComponent

    init {
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.cellRenderer = SceneTreeCellRenderer()
        component = JScrollPane(tree)
    }

    fun isEnabled(): Boolean {
        return try {
            Registry.`is`("gdscript.debug.sceneTree.enabled", true)
        } catch (_: Exception) {
            true
        }
    }

    override fun dispose() {
        model.clear()
    }
}

private class SceneTreeCellRenderer : javax.swing.tree.DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: javax.swing.JTree,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ): java.awt.Component {
        val component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        val node = value as? DefaultMutableTreeNode ?: return component
        val info = node.userObject as? SceneTreeNodeInfo ?: return component
        text = info.toString()
        if (info.isActive) {
            font = font.deriveFont(java.awt.Font.BOLD)
        }
        return component
    }
}
