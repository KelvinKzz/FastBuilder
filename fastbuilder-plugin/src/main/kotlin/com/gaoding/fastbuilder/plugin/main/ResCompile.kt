package com.gaoding.fastbuilder.plugin.main

import com.gaoding.fastbuilder.lib.utils.FileUtil
import com.gaoding.fastbuilder.lib.utils.Log
import com.gaoding.fastbuilder.plugin.util.BuildUtils
import org.gradle.api.Project

open class ResCompile {

    lateinit var project: Project

    fun collectRes() {
        BuildUtils.getAndroid(project)?.applicationVariants?.all { variant ->
            val variantName = variant.name.capitalize()
            val generateSources = project.tasks.findByName("process${variantName}Resources")
            generateSources?.doLast {
                Log.i("复制资源，task " + generateSources.name)
                for (f in generateSources.outputs.files.files) {
                    Log.i("资源文件:" + f.path)
                    val path = BuildUtils.getFastBuildResourcesPath(project)//缓存
                    if (f.isFile) {
                        FileUtil.fileCopy(f.path, path + "/" + f.name)
                        if (f.name.contains("resources-debug.ap_")) {
                            FileUtil.fileCopy(
                                f.path,
                                "$path/" + (f.name.replace(
                                    "resources-debug.ap_",
                                    "patch_resources.apk"
                                ))
                            )
                        }
                    }
                }
            }
        }
    }

}