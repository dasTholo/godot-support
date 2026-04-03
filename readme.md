# Godot Support for RustRover

Community fork of [JetBrains/godot-support](https://github.com/JetBrains/godot-support) for **RustRover**.

## GdScript Plugin

- Located in the `gdscript` folder
- Supports Godot 4.6 and higher
- GDScript language support (parser, completion, references, inspections)
- Godot LSP integration (auto-connect to Godot editor)
- DAP-based GDScript debugging
- GDExtension/Rust navigation
- Scene file (`.tscn`) support

[Detailed readme](gdscript/README.md)

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

## License

[MIT License](LICENSE)

Based on:
- [gdscript](https://github.com/penguinencounter/gdscript) by David Horaček (MIT)
- [godot-support](https://github.com/JetBrains/godot-support) by JetBrains s.r.o. (Apache 2.0)

## Contributing

This project welcomes contributions and suggestions.
Please refer to our [Guidelines](CONTRIBUTING.md) for contributing.
