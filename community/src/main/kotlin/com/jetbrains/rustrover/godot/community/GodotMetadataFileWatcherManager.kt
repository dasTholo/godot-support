package com.jetbrains.rustrover.godot.community

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rustrover.godot.community.actions.GodotToolbarUpdateService
import com.jetbrains.rustrover.godot.community.utils.GodotCommunityUtil

class GodotMetadataFileWatcherManager : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Initialize the lazy service
        GodotToolbarUpdateService.getInstance(project)
        val metadataService = GodotMetadataService.getInstance(project)

        GodotCommunityUtil.getGodotProjectBasePathFlow(project).collect { basePath ->
            if (basePath == null) {
                metadataService.stopWatcher()
            } else {
                metadataService.startWatcher(basePath)
            }
        }
    }
}
