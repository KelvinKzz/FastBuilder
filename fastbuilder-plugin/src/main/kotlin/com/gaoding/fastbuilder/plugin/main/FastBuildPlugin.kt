package com.gaoding.fastbuilder.plugin.main

import com.gaoding.fastbuilder.lib.utils.FileUtil
import com.gaoding.fastbuilder.lib.utils.Log
import com.gaoding.fastbuilder.plugin.extension.BuilderExtension
import com.gaoding.fastbuilder.plugin.transform.InjectTransform
import com.gaoding.fastbuilder.plugin.util.BuildUtils
import com.gaoding.fastbuilder.plugin.util.BuilderInitializer
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.lang.Exception

class FastBuildPlugin : Plugin<Project> {

    companion object {
        lateinit var builderExtension: BuilderExtension
    }

    private fun isEnable(project: Project): Boolean {
        val enableFile =
            File(project.rootProject.rootDir.absolutePath + File.separator + ".gradle" + File.separator + "FastBuildEnable")
        return enableFile.exists()
    }

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            Log.i("不是application工程")
            throw Exception("必须是application工程")
        }
        builderExtension = project.extensions.create("builder", BuilderExtension::class.java)
        if (!isEnable(project)) {
            return
        }

        BuildUtils.getAndroid(project)?.registerTransform(InjectTransform(project))
        project.dependencies.add("implementation", "com.gaoding.fastbuilder:hotpatch:1.0.0")

        start(project)
    }

    private fun start(project: Project) {
        project.rootProject.subprojects { subproject ->
            subproject.afterEvaluate {
                BuilderInitializer.getSourcePath(it)
            }
        }
        project.afterEvaluate {
            if (!builderExtension.enable) {
                return@afterEvaluate
            }
            Log.i("========================")
            Log.i("FastBuilder插件启动 " + project.name)
            Log.i("========================")

            Log.i(builderExtension.toString())
            doTask(project)
        }
    }

    private fun hasCompile(project: Project): Boolean {
        val infoTxt = BuildUtils.getFastBuildPath(project) + "/java_info.txt"
        return File(infoTxt).exists()
    }

    private fun doTask(project: Project) {
        val compile: BaseCompile
        val hasAssemble = project.rootProject.gradle.startParameter.taskNames.find { it.contains("assemble") || it.contains("runBuild") } != null
        if (!hasAssemble) {
            return
        }
        if (false && hasCompile(project)) {
            Log.i("增量编译")
            compile = IncrementalCompile()
        } else {
            Log.i("资源收集")
            FileUtil.deleteDir(File(BuildUtils.getFastBuildPath(project)))
            FileUtil.ensumeDir(File(BuildUtils.getFastBuildPath(project)))

            project.gradle.addListener(TaskCollectListener(project))

            compile = AllCompile()
        }

        compile.project = project
        compile.apply()
    }
}