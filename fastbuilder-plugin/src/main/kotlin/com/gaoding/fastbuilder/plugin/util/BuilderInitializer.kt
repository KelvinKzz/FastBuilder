package com.gaoding.fastbuilder.plugin.util

import com.android.build.gradle.AndroidConfig
import com.gaoding.fastbuilder.lib.utils.FileUtil
import com.gaoding.fastbuilder.lib.utils.Log
import com.gaoding.fastbuilder.lib.utils.StringUtil
import com.google.gson.Gson
import org.gradle.api.Project
import org.json.simple.JSONObject
import java.io.File
import java.util.*

object BuilderInitializer {

    private val projectDescription = LinkedHashMap<String, Any?>()

    private lateinit var sProject:Project
    val javaSet = HashSet<String>()
    val resSet = HashSet<String>()
    val packageNameSet = HashSet<String>()

    fun clear() {
        javaSet.clear()
        resSet.clear()
        packageNameSet.clear()
    }

    fun init(project: Project) {
        val android = BuildUtils.getAndroid(project) ?: return
        sProject = project
        projectDescription["project_type"] = "gradle"
        projectDescription["out_dir"] = BuildUtils.getFastBuildPath(project)
        projectDescription["build_tool_dir"] = BuildUtils.getBuildToolPath(project)
        projectDescription["java_home"] = getJavaHome()
        projectDescription["root_dir"] = project.rootDir.path
        projectDescription["main_project_name"] = project.name
        projectDescription["build_directory"] = project.buildDir.path
        projectDescription["build_tools_version"] = android.buildToolsVersion
        projectDescription["sdk_directory"] = android.sdkDirectory.path
        projectDescription["build_tools_directory"] =
            joinPath(arrayOf(android.sdkDirectory.path, "build-tools", android.buildToolsVersion))
        projectDescription["compile_sdk_version"] = android.compileSdkVersion
        android.compileSdkVersion?.let {
            projectDescription["compile_sdk_directory"] =
                joinPath(arrayOf(android.sdkDirectory.path, "platforms", it))
        }
        projectDescription["package_name_manifest"] = BuildUtils.getPackageName(project)
        projectDescription["main_manifest_path"] = BuildUtils.getAndroidManifestPath(project)
        projectDescription["boot_activity"] = BuildUtils.getBootActivity(project)

//        saveJson(project)
    }

    fun updateBootActivity(project: Project) {
        if (StringUtil.isEmpty(projectDescription["boot_activity"] as String)) {
            projectDescription["boot_activity"] = BuildUtils.getBootActivity(project)
        }
    }

    fun saveJson(project: Project) {
        projectDescription["scan_src"] = change(javaSet)
        projectDescription["scan_res"] = change(resSet)
        projectDescription["package_name"] = change(packageNameSet)
        val json = JSONObject.toJSONString(projectDescription)
        val savaPath = BuildUtils.getFastBuildPath(project) + "/build_info.json"
        FileUtil.ensumeDir(File(BuildUtils.getFastBuildPath(project)))
        FileUtil.writeFile(savaPath, json)
    }

    // from retrolambda
    fun getJavaHome(): String {

        val bJson: String =
            FileUtil.readContents(BuildUtils.getBuildToolPath(sProject) + "/config.json")
        val gson = Gson()

        val sBuilderConfig: BuilderConfig = gson.fromJson(bJson, BuilderConfig::class.java)
        sBuilderConfig.java_path?.let {
            Log.i("javaHomeProp: $it")
            return it
        }
        val javaHomeProp = System.getProperty("java.home")
        Log.i("javaHomeProp: $javaHomeProp")
        return if (javaHomeProp != null) {
            val jreIndex = javaHomeProp.lastIndexOf("${File.separator}jre")
            if (jreIndex != -1) {
                javaHomeProp.substring(0, jreIndex)
            } else {
                javaHomeProp
            }
        } else {
            System.getenv("JAVA_HOME")
        }
    }

    private fun appendDirs(targetCollections: HashSet<String>?, collections: MutableSet<File>?) {
        if (collections != null) {
            for (dir in collections) {
                targetCollections?.add(dir.absolutePath)
            }
        }
    }

    private fun change(collections: MutableSet<String>): List<String> {
        return ArrayList<String>(collections)
    }

    private fun joinPath(sep: Array<String>): String {
        if (sep.isEmpty()) {
            return ""
        }
        if (sep.size == 1) {
            return sep[0]
        }

        return File(sep[0], joinPath(sep.copyOfRange(1, sep.size))).path
    }

    fun getSourcePath(project: Project) {
        try {
            if (project.hasProperty("android")) {
                val android = project.extensions.getByName("android") as AndroidConfig
                val sourceSetsValue = android.sourceSets.findByName("main")
                Log.i("java path " + android.defaultConfig.applicationId + " " + sourceSetsValue?.java?.srcDirs)
                if (!StringUtil.isEmpty(android.defaultConfig.applicationId)) {
                    packageNameSet.add(android.defaultConfig.applicationId)
                }
                appendDirs(javaSet, sourceSetsValue?.java?.srcDirs)//获取java目录
                appendDirs(resSet, sourceSetsValue?.res?.srcDirs)//获取res目录
            }
        } catch (e: Exception) {

        }
    }
}
