package com.gaoding.fastbuilder.plugin.main

import com.gaoding.fastbuilder.plugin.util.BuildUtils
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState


/**
 * 收集jar路径
 */
class TaskCollectListener(private var mProject: Project) : TaskExecutionListener, BuildListener {

    override fun beforeExecute(task: Task) {
        BuildUtils.getAndroid(mProject)?.applicationVariants?.all { variant ->
            val variantName = variant.name.capitalize()
            if (task.name.contains("compile${variantName}JavaWithJavac")) {
                for (f in task.inputs.files.files) {
                    if (f.path.endsWith(".jar")) {
                        JarClassPathManager.addClassPath(f.path)
                    }
                }
            }
        }
    }

    override fun afterExecute(task: Task, state: TaskState) {
        BuildUtils.getAndroid(mProject)?.applicationVariants?.all { variant ->
            val variantName = variant.name.capitalize()
            if (task.name.contains("compile${variantName}JavaWithJavac")) {
                for (f in task.inputs.files.files) {
                    if (f.path.endsWith(".jar")) {
                        JarClassPathManager.addClassPath(f.path)
                    }
                }
            }
        }
    }

    override fun buildStarted(gradle: Gradle) {
    }

    override fun settingsEvaluated(settings: Settings) {
    }

    override fun projectsLoaded(gradle: Gradle) {
    }

    override fun projectsEvaluated(gradle: Gradle) {
    }

    override fun buildFinished(result: BuildResult) {
    }


}