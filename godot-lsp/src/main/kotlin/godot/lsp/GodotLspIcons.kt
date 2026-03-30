package godot.lsp

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object GodotLspIcons {
    @JvmField val GodotLogo: Icon = IconLoader.getIcon("/icons/Godot.svg", GodotLspIcons::class.java)
    @JvmField val GodotDisconnected: Icon = IconLoader.getIcon("/icons/GodotDisconnected.svg", GodotLspIcons::class.java)
}
