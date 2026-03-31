import com.intellij.openapi.util.IconLoader
import com.intellij.rustrover.plugins.godot.gdscript.icons.RustRoverPluginsGodotGdscriptIcons

// IJ will automatically add `_dark` to the basename of the SVG file if in Dark theme.

class GdScriptPluginIcons {
    class Icons {
        companion object {
            val GodotLogo = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Godot
            val BackupIcon = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Object
            val GodotFile = RustRoverPluginsGodotGdscriptIcons.GodotEditor.GodotFile
            val GodotConfigFile = RustRoverPluginsGodotGdscriptIcons.GodotEditor.FileDialog
            val GodotProjectFile = RustRoverPluginsGodotGdscriptIcons.GodotEditor.FileDialog
            val GDScript = IconLoader.getIcon("icons/GDScript.svg", GdScriptPluginIcons::class.java)
            val Godot = IconLoader.getIcon("icons/Godot.svg", GdScriptPluginIcons::class.java)
            val GodotDisconnected = IconLoader.getIcon("icons/GodotDisconnected.svg", GdScriptPluginIcons::class.java)
        }
    }

    class GDScriptIcons{
        companion object{
            val METHOD_MARKER = RustRoverPluginsGodotGdscriptIcons.GodotEditor.MemberMethod
            val VAR_MARKER = RustRoverPluginsGodotGdscriptIcons.GodotEditor.MemberProperty
            val CONST_MARKER = RustRoverPluginsGodotGdscriptIcons.GodotEditor.MemberConstant
            val ENUM_MARKER = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Enum
            val SIGNAL_MARKER = RustRoverPluginsGodotGdscriptIcons.GodotEditor.MemberSignal
            val OBJECT = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Object
            val SIGNAL = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Signal
            val SLOT = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Slot
            val LINK = RustRoverPluginsGodotGdscriptIcons.GodotEditor.LinkButton
            val ERROR = RustRoverPluginsGodotGdscriptIcons.GodotEditor.StatusError
            val RESOURCE = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Mesh
            val STRING = RustRoverPluginsGodotGdscriptIcons.GodotEditor.String
            val OVERRIDE = RustRoverPluginsGodotGdscriptIcons.GodotEditor.MethodOverride
            val ANIMATION = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Animation
            val NODE = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Node
        }
    }

    class TscnIcons {
        companion object {
            val FILE = RustRoverPluginsGodotGdscriptIcons.TscnFile
            val InstanceOptions = RustRoverPluginsGodotGdscriptIcons.GodotEditor.InstanceOptions
            val Script = RustRoverPluginsGodotGdscriptIcons.GodotEditor.Script
            val SceneUniqueName = RustRoverPluginsGodotGdscriptIcons.GodotEditor.SceneUniqueName
            val GuiVisibilityXray = RustRoverPluginsGodotGdscriptIcons.GodotEditor.GuiVisibilityXray
            val GuiVisibilityVisible = RustRoverPluginsGodotGdscriptIcons.GodotEditor.GuiVisibilityVisible
            val GuiVisibilityVisibleDark = RustRoverPluginsGodotGdscriptIcons.GodotEditor.GuiVisibilityVisibleDark
            val GuiVisibilityHidden = RustRoverPluginsGodotGdscriptIcons.GodotEditor.GuiVisibilityHidden
            val GuiVisibilityHiddenDark = RustRoverPluginsGodotGdscriptIcons.GodotEditor.GuiVisibilityHiddenDark
        }
    }
}