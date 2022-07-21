package com.gaoding.fastbuilder.plugin.main

import com.gaoding.fastbuilder.lib.utils.FileUtil
import com.gaoding.fastbuilder.plugin.util.BuildUtils
import org.gradle.api.Project;


/**
 * Jar class路径，用于kotlinc/javac编译使用
 * 基本涵盖两块
 * 1.本地模块class
 * 2.三方库class
 */
object JarClassPathManager {

    private val mClassPath = HashSet<String>()

    fun addClassPath(path: String) {
        mClassPath.add(path)
    }

    fun writeFile(project: Project) {
        FileUtil.writeFile(mClassPath, BuildUtils.getFastBuildPath(project) + "/jar_list.txt")
    }

}