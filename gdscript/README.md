<div style="display: flex; align-items: center;">
    <a href="https://godotengine.org">
        <img src="screens/godot.svg" alt="Godot" width="96">
    </a>
    <img src="screens/plus.png">
    <a href="https://www.jetbrains.com/rust">
        <img src="screens/jb_beam.svg" alt="RustRover" width="96">
    </a>
</div>

GDScript language plugin for RustRover — community fork of [JetBrains/godot-support](https://github.com/JetBrains/godot-support)  
Supports Godot 4.6 and higher

## History

This plugin was originally developed by David ([@IceExplosive](https://gitlab.com/IceExplosive)) and was available on
GitLab and the JetBrains Marketplace. When the original author could no longer maintain the project, JetBrains took over
its development. This community fork by DasTholo continues development with a focus on RustRover and Godot 4.6+.

[Settings](documentation%2Fsettings.md)

[List of features](documentation%2Ffeatures%2Ffeatures.md)

## Recommended Godot Editor Settings

When creating or editing GDScript files externally (in RustRover), Godot needs to detect these changes to generate `.uid` files and update its internal caches. Without this, you may see errors like *"Unrecognized UID"* or *"Could not find base class"* until the Godot editor is restarted.

To enable automatic background rescanning, open **Editor → Editor Settings** in Godot and enable:

| Setting | Path | Effect |
|---------|------|--------|
| Import resources when unfocused | `interface/editor/import_resources_when_unfocused` | Godot rescans the filesystem in the background, even without window focus |
| Auto reload scripts on external change | `text_editor/behavior/files/auto_reload_scripts_on_external_change` | Automatically reloads scripts modified outside the editor |

> **Note:** These are global Godot editor settings, not per-project settings. They apply to all projects.

## GdScript toolkit (not related to this project)

An independent set of tools for GdScript (Formatter, Linter, and more) that can work together with this plugin.  
For example instead of plugin's built-in formatter, you can use theirs 
<details>
    <summary>gdformat</summary>

- Install by their own tutorial
- Add File Watcher `Settings -> Tools -> File Watchers`
- File type: `GdScript language file`
- Scope: `Project files`
- Program: `/home/{username}/.local/bin/gdformat`
- Arguments: `-l 160 $FilePath$`
- Output paths to refresh: `$FilePath$`
- Enable Auto-save edited files to trigger the watcher
- Thanks to @e.sirkova for mentioning it.
</details>
