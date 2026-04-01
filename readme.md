[![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)](https://github.com/JetBrains#jetbrains-on-github)

# Godot Support for RustRover

Community fork of [JetBrains/godot-support](https://github.com/JetBrains/godot-support) for **RustRover** and other non-Rider JetBrains IDEs.

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

### RustRover / Other JetBrains IDEs

Build from source (requires JDK 21):

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
