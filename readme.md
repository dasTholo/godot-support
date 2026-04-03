# Godot Support for RustRover

Community fork of [JetBrains/godot-support](https://github.com/JetBrains/godot-support) for **RustRover**.

<div style="display: flex; align-items: center;">
    <a href="https://godotengine.org">
        <img src="gdscript/screens/godot.svg" alt="Godot" width="96">
    </a>
    <img src="gdscript/screens/plus.png">
    <a href="https://www.jetbrains.com/rust">
        <img src="gdscript/screens/jb_beam.svg" alt="RustRover" width="96">
    </a>
</div>

GDScript language plugin for RustRover — supports Godot 4.6 and higher.

## Features

- GDScript language support (parser, completion, references, inspections)
- Godot LSP integration (auto-connect to Godot editor)
- DAP-based GDScript debugging
- GDExtension/Rust navigation
- Scene file (`.tscn`) support

[Settings](gdscript/documentation/settings.md) · [List of features](gdscript/documentation/features/features.md)

## Installation

### From Release (recommended)

1. Download the latest `rustrover-gdscript.zip` from [Releases](https://github.com/dasTholo/godot-support/releases)
2. In RustRover: **Settings → Plugins → ⚙️ → Install Plugin from Disk…**
3. Select the downloaded `.zip` file and restart the IDE

### Build from Source

Requires JDK 21:

```bash
cd gdscript && ./gradlew buildPlugin
# Result: gdscript/build/distributions/rustrover-gdscript.zip
```

## Recommended Godot Editor Settings

When creating or editing GDScript files externally (in RustRover), Godot needs to detect these changes to generate `.uid` files and update its internal caches. Without this, you may see errors like *"Unrecognized UID"* or *"Could not find base class"* until the Godot editor is restarted.

To enable automatic background rescanning, open **Editor → Editor Settings** in Godot and enable:

| Setting | Path | Effect |
|---------|------|--------|
| Import resources when unfocused | `interface/editor/import_resources_when_unfocused` | Godot rescans the filesystem in the background, even without window focus |
| Auto reload scripts on external change | `text_editor/behavior/files/auto_reload_scripts_on_external_change` | Automatically reloads scripts modified outside the editor |

> **Note:** These are global Godot editor settings, not per-project settings. They apply to all projects.

## History

This plugin was originally developed by David ([@IceExplosive](https://gitlab.com/IceExplosive)) and was available on
GitLab and the JetBrains Marketplace. When the original author could no longer maintain the project, JetBrains took over
its development. This community fork by DasTholo continues development with a focus on RustRover and Godot 4.6+.

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

## License

[MIT License](LICENSE)

Based on:
- [gdscript](https://github.com/penguinencounter/gdscript) by David Horaček (MIT)
- [godot-support](https://github.com/JetBrains/godot-support) by JetBrains s.r.o. (Apache 2.0)

## Contributing

This project welcomes contributions and suggestions.
Please refer to our [Guidelines](gdscript/CONTRIBUTING.md) for contributing.
