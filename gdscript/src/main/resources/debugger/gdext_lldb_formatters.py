"""
LLDB Pretty-Printers for godot-rust/gdext types.

Auto-loaded by the Godot Support plugin when a gdext project is detected.
Requires debug symbols (debug = true in Cargo.toml profile).
"""

import lldb


# --- Summary Providers (one-line display) ---

def summary_gstring(valobj, internal_dict):
    """GString / StringName -> string content"""
    try:
        string_data = valobj.GetChildMemberWithName("opaque")
        if not string_data.IsValid():
            return valobj.GetValue() or "<GString>"
        inner = string_data.GetChildAtIndex(0)
        if inner.IsValid():
            summary = inner.GetSummary()
            if summary:
                return summary
        return valobj.GetValue() or "<GString>"
    except Exception:
        return "<GString: error>"


def summary_vector2(valobj, internal_dict):
    """Vector2 -> (x, y)"""
    try:
        x = valobj.GetChildMemberWithName("x").GetValue()
        y = valobj.GetChildMemberWithName("y").GetValue()
        return f"({x}, {y})"
    except Exception:
        return "<Vector2: error>"


def summary_vector3(valobj, internal_dict):
    """Vector3 -> (x, y, z)"""
    try:
        x = valobj.GetChildMemberWithName("x").GetValue()
        y = valobj.GetChildMemberWithName("y").GetValue()
        z = valobj.GetChildMemberWithName("z").GetValue()
        return f"({x}, {y}, {z})"
    except Exception:
        return "<Vector3: error>"


def summary_vector4(valobj, internal_dict):
    """Vector4 -> (x, y, z, w)"""
    try:
        x = valobj.GetChildMemberWithName("x").GetValue()
        y = valobj.GetChildMemberWithName("y").GetValue()
        z = valobj.GetChildMemberWithName("z").GetValue()
        w = valobj.GetChildMemberWithName("w").GetValue()
        return f"({x}, {y}, {z}, {w})"
    except Exception:
        return "<Vector4: error>"


def summary_color(valobj, internal_dict):
    """Color -> (r, g, b, a)"""
    try:
        r = valobj.GetChildMemberWithName("r").GetValue()
        g = valobj.GetChildMemberWithName("g").GetValue()
        b = valobj.GetChildMemberWithName("b").GetValue()
        a = valobj.GetChildMemberWithName("a").GetValue()
        return f"({r}, {g}, {b}, {a})"
    except Exception:
        return "<Color: error>"


def summary_gd_object(valobj, internal_dict):
    """Gd<T> -> ClassName#instance_id"""
    try:
        raw = valobj.GetChildMemberWithName("raw")
        if raw.IsValid():
            obj = raw.GetChildMemberWithName("obj")
            if obj.IsValid():
                instance_id = obj.GetChildMemberWithName("instance_id")
                if instance_id.IsValid():
                    type_name = valobj.GetType().GetName()
                    start = type_name.find("<")
                    end = type_name.rfind(">")
                    if start >= 0 and end > start:
                        class_name = type_name[start + 1:end]
                    else:
                        class_name = type_name
                    return f"{class_name}#{instance_id.GetValueAsUnsigned()}"
        return valobj.GetValue() or "<Gd<T>>"
    except Exception:
        return "<Gd: error>"


def summary_node_path(valobj, internal_dict):
    """NodePath -> path string"""
    try:
        opaque = valobj.GetChildMemberWithName("opaque")
        if opaque.IsValid():
            summary = opaque.GetSummary()
            if summary:
                return summary
        return valobj.GetValue() or "<NodePath>"
    except Exception:
        return "<NodePath: error>"


def summary_variant(valobj, internal_dict):
    """Variant -> type: value (best-effort)"""
    try:
        opaque = valobj.GetChildMemberWithName("opaque")
        if opaque.IsValid():
            return opaque.GetSummary() or "<Variant>"
        return "<Variant>"
    except Exception:
        return "<Variant: error>"


def summary_string_name(valobj, internal_dict):
    """StringName -> string content"""
    return summary_gstring(valobj, internal_dict)


def summary_transform2d(valobj, internal_dict):
    """Transform2D -> x, y, origin"""
    try:
        a = valobj.GetChildMemberWithName("a")
        b = valobj.GetChildMemberWithName("b")
        origin = valobj.GetChildMemberWithName("origin")
        ax = a.GetChildMemberWithName("x").GetValue() if a.IsValid() else "?"
        ay = a.GetChildMemberWithName("y").GetValue() if a.IsValid() else "?"
        bx = b.GetChildMemberWithName("x").GetValue() if b.IsValid() else "?"
        by = b.GetChildMemberWithName("y").GetValue() if b.IsValid() else "?"
        ox = origin.GetChildMemberWithName("x").GetValue() if origin.IsValid() else "?"
        oy = origin.GetChildMemberWithName("y").GetValue() if origin.IsValid() else "?"
        return f"x: ({ax}, {ay}), y: ({bx}, {by}), origin: ({ox}, {oy})"
    except Exception:
        return "<Transform2D: error>"


# --- Registration ---

def __lldb_init_module(debugger, internal_dict):
    """Called by LLDB when this script is imported."""
    prefix = "godot_core::builtin::"

    commands = [
        # Strings
        (f"{prefix}string::GString", "summary_gstring"),
        (f"{prefix}string_name::StringName", "summary_string_name"),
        (f"{prefix}node_path::NodePath", "summary_node_path"),
        # Vectors
        (f"{prefix}vectors::vector2::Vector2", "summary_vector2"),
        (f"{prefix}vectors::vector2i::Vector2i", "summary_vector2"),
        (f"{prefix}vectors::vector3::Vector3", "summary_vector3"),
        (f"{prefix}vectors::vector3i::Vector3i", "summary_vector3"),
        (f"{prefix}vectors::vector4::Vector4", "summary_vector4"),
        (f"{prefix}vectors::vector4i::Vector4i", "summary_vector4"),
        # Color
        (f"{prefix}color::Color", "summary_color"),
        # Transform
        (f"{prefix}transform2d::Transform2D", "summary_transform2d"),
        # Variant
        (f"{prefix}variant::Variant", "summary_variant"),
    ]

    for type_name, func_name in commands:
        debugger.HandleCommand(
            f'type summary add -F {__name__}.{func_name} "{type_name}"'
        )

    # Gd<T> uses regex matching for generic types
    debugger.HandleCommand(
        f'type summary add -x "^godot_core::obj::gd::Gd<.*>$" '
        f'-F {__name__}.summary_gd_object'
    )
