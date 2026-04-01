package gdscript.dap.scenetree

import com.intellij.openapi.util.registry.Registry
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

data class SceneTreeNodeInfo(
    val name: String,
    val typeName: String,
    val isActive: Boolean = false,
) {
    override fun toString(): String = "$name ($typeName)"
}

class GdScriptSceneTreeModel : DefaultTreeModel(DefaultMutableTreeNode("Loading...")) {

    val maxDepth: Int
        get() = try {
            Registry.intValue("gdscript.debug.sceneTree.maxDepth", 10)
        } catch (_: Exception) {
            10
        }

    fun updateRoot(rootNode: SceneTreeNodeInfo, children: List<Pair<SceneTreeNodeInfo, List<SceneTreeNodeInfo>>>) {
        val rootTreeNode = DefaultMutableTreeNode(rootNode)
        for ((child, grandchildren) in children) {
            val childNode = DefaultMutableTreeNode(child)
            for (gc in grandchildren) {
                childNode.add(DefaultMutableTreeNode(gc))
            }
            rootTreeNode.add(childNode)
        }
        setRoot(rootTreeNode)
        reload()
    }

    fun clear() {
        setRoot(DefaultMutableTreeNode("No active debug session"))
        reload()
    }
}
