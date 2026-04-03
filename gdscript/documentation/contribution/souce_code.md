# Source code

Source code is **mostly** in Kotlin - Java files are simply old or there was some bug when trying to move them into Kotlin, but all new code should be written in Kotlin.  

The plugin is a single unified module with the following source packages under `src/main/kotlin/`:

- gdscript  
This is the core for parsing .gd files, allowing auto-completion, grammar checks, highlights and all those fancy features we love.
- tscn  
Is for parsing .tscn files which contains information about Godot Node's structure. f.e. access child nodes in script via `$Label`  
.tscn files contain a list of those children and their types which are used for code completion
- project  
This parser handles the base file of all Godot projects: `project.godot`  
It marks root of project (in case you open parent folder of multiple Godot projects - but it's still buggy and not recommended).  
Contains all Autoloaded scripts to add them into completions or list of input names f.e.: "up", "down", "right", "left"

Within each package, sub-folders are mostly separating IDEA features - except for `psi` which is the base for all other features. Usually you can just check / set breakpoints with any of those files.  

There isn't a simple flow to follow as each feature is invoked by the editor itself based on user actions - f.e. completions do not execute until you press ctrl+space or wait until it's invoked automatically.
