package gdscript.dap.presentation.presenters

import GdScriptPluginIcons
import gdscript.dap.presentation.GdScriptValuePresenter
import javax.swing.Icon

class NodePresenter : GdScriptValuePresenter {

    override fun canPresent(type: String): Boolean = type in KNOWN_NODE_TYPES

    override fun formatValue(type: String, value: String): String {
        val match = INSTANCE_PATTERN.find(value)
        if (match != null) {
            val id = match.groupValues[1]
            return "$type #$id"
        }
        return "$type: $value"
    }

    override fun getIcon(type: String, value: String): Icon =
        GdScriptPluginIcons.GDScriptIcons.NODE

    companion object {
        private val INSTANCE_PATTERN = Regex("""<\w+#(\d+)>""")

        val KNOWN_NODE_TYPES = setOf(
            "Node", "Node2D", "Node3D",
            "CharacterBody2D", "RigidBody2D", "StaticBody2D", "AnimatableBody2D", "Area2D",
            "CollisionShape2D", "CollisionPolygon2D", "RayCast2D", "ShapeCast2D",
            "PhysicalBone2D", "Skeleton2D", "Bone2D",
            "CharacterBody3D", "RigidBody3D", "StaticBody3D", "AnimatableBody3D", "Area3D",
            "CollisionShape3D", "CollisionPolygon3D", "RayCast3D", "ShapeCast3D",
            "PhysicalBone3D", "Skeleton3D", "BoneAttachment3D",
            "Sprite2D", "AnimatedSprite2D", "Camera2D", "CanvasGroup", "CanvasModulate",
            "Light2D", "PointLight2D", "DirectionalLight2D", "LightOccluder2D",
            "Line2D", "Path2D", "PathFollow2D", "Polygon2D",
            "TileMap", "TileMapLayer", "Parallax2D", "ParallaxBackground", "ParallaxLayer",
            "NavigationRegion2D", "NavigationAgent2D", "NavigationLink2D", "NavigationObstacle2D",
            "MeshInstance3D", "MultiMeshInstance3D", "Camera3D",
            "DirectionalLight3D", "OmniLight3D", "SpotLight3D",
            "GPUParticles3D", "CPUParticles3D", "GPUParticles2D", "CPUParticles2D",
            "Path3D", "PathFollow3D", "Marker3D",
            "WorldEnvironment", "FogVolume", "VoxelGI", "LightmapGI",
            "NavigationRegion3D", "NavigationAgent3D", "NavigationLink3D", "NavigationObstacle3D",
            "CSGBox3D", "CSGCylinder3D", "CSGMesh3D", "CSGPolygon3D", "CSGSphere3D", "CSGTorus3D", "CSGCombiner3D",
            "Control", "Container", "MarginContainer", "HBoxContainer", "VBoxContainer",
            "GridContainer", "CenterContainer", "PanelContainer", "ScrollContainer",
            "SplitContainer", "HSplitContainer", "VSplitContainer", "TabContainer",
            "SubViewportContainer", "AspectRatioContainer", "FlowContainer",
            "Label", "RichTextLabel", "Button", "TextureButton", "LinkButton",
            "CheckBox", "CheckButton", "MenuButton", "OptionButton",
            "LineEdit", "TextEdit", "CodeEdit", "SpinBox",
            "ProgressBar", "HSlider", "VSlider", "HScrollBar", "VScrollBar",
            "Tree", "ItemList", "TabBar", "MenuBar", "Panel",
            "TextureRect", "NinePatchRect", "ColorRect", "VideoStreamPlayer",
            "FileDialog", "AcceptDialog", "ConfirmationDialog", "Popup", "PopupMenu", "PopupPanel",
            "ColorPicker", "ColorPickerButton",
            "AudioStreamPlayer", "AudioStreamPlayer2D", "AudioStreamPlayer3D",
            "AudioListener2D", "AudioListener3D",
            "AnimationPlayer", "AnimationTree", "AnimationMixer", "Tween",
            "Timer", "CanvasLayer", "SubViewport", "Viewport", "Window",
            "HTTPRequest", "MultiplayerSpawner", "MultiplayerSynchronizer",
            "XRCamera3D", "XRController3D", "XROrigin3D",
            "PinJoint2D", "GrooveJoint2D", "DampedSpringJoint2D",
            "HingeJoint3D", "SliderJoint3D", "ConeTwistJoint3D", "Generic6DOFJoint3D",
        )
    }
}
