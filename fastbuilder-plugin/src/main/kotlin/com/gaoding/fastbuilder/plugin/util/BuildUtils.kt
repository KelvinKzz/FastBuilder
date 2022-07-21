package com.gaoding.fastbuilder.plugin.util

import com.android.build.gradle.AndroidConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.builder.model.Version
import com.gaoding.fastbuilder.lib.utils.FileUtil
import com.gaoding.fastbuilder.lib.utils.Log
import com.gaoding.fastbuilder.lib.utils.StringUtil
import com.gaoding.fastbuilder.plugin.main.JarClassPathManager
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object BuildUtils {

    fun getExternalCacheDir(project: Project): String {
        return "sdcard/Android/data/" + getPackageName(project) + "/cache"
    }

    fun getBuildPath(project: Project): String {
        return project.buildDir.absolutePath
    }

    fun getRootProjectPath(project: Project): String {
        return project.rootDir.absolutePath
    }

    fun getBuildToolPath(project: Project): String {
        return getRootProjectPath(project) + "/fastbuilder_tool"
    }

    fun getFastBuildPath(project: Project): String {
        File(getRootProjectPath(project) + "/build/fastbuild").mkdir()
        return getRootProjectPath(project) + "/build/fastbuild"
    }

    fun getFastBuildResourcesPath(project: Project): String {
        File(getFastBuildPath(project) + "/resources").mkdir()
        return getFastBuildPath(project) + "/resources"
    }

    fun getAndroidJarPath(project: Project): String {
        return "${getSdkDirectory(project)}${File.separator}platforms${File.separator}${
            getAndroid(
                project
            )?.compileSdkVersion
        }${File.separator}android.jar"
    }

    /**
     * 获取sdk路径
     * @param project
     * @return
     */
    fun getSdkDirectory(project: Project): String {
        var sdkDirectory = getAndroid(project)?.sdkDirectory?.absolutePath
        if (sdkDirectory?.contains("/") == true) {
            sdkDirectory = sdkDirectory.replace("/", "/")
        }
        return sdkDirectory ?: ""
    }

    /**
     * 获取dx命令路径
     * @param project
     * @return
     */
    fun getDxCmdPath(project: Project): String {
        val dx = File(
            getSdkDirectory(project),
            "build-tools${File.separator}${getAndroid(project)?.buildToolsVersion}${File.separator}dx"
        )
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${dx.absolutePath}.bat"
        }
        return dx.absolutePath
    }

    /**
     * 获取d8命令路径
     * @param project
     * @return
     */
    fun getD8CmdPath2(project: Project): String {
        val dx = File(
            getSdkDirectory(project),
            "build-tools${File.separator}${getAndroid(project)?.buildToolsVersion}${File.separator}d8"
        )
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${dx.absolutePath}.bat"
        }
        return dx.absolutePath
    }

    fun getD8CmdPath(project: Project): String {
        val dx =
            File(getSdkDirectory(project), "build-tools${File.separator}${getAndroid(project)?.buildToolsVersion}${File.separator}d8")
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${dx.absolutePath}.bat"
        }
        return dx.absolutePath
    }

    /**
     * 获取aapt命令路径
     * @param project
     * @return
     */
    fun getAaptCmdPath(project: Project): String {
        val aapt = File(
            getSdkDirectory(project),
            "build-tools${File.separator}${getAndroid(project)?.buildToolsVersion}${File.separator}aapt"
        )
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${aapt.absolutePath}.exe"
        }
        return aapt.absolutePath
    }

    /**
     * 获取aapt2命令路径
     * @param project
     * @return
     */
    fun getAapt2CmdPath(project: Project): String {
        val aapt = File(
            getSdkDirectory(project),
            "build-tools${File.separator}${getAndroid(project)?.buildToolsVersion}${File.separator}aapt2"
        )
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${aapt.absolutePath}.exe"
        }
        return aapt.absolutePath
    }

    /**
     * 获取adb命令路径
     * @param project
     * @return
     */
    fun getAdbCmdPath(project: Project): String {
        val adb = File(getSdkDirectory(project), "platform-tools${File.separator}adb")
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${adb.absolutePath}.exe"
        }
        return adb.absolutePath
    }


    fun getAndroidManifestPath(project: Project): String {
        val android = project.extensions.findByType(AppExtension::class.java)
        val main: AndroidSourceSet? = android?.sourceSets?.getByName("main")
        return main?.manifest?.srcFile?.path.toString()
    }

    fun getAndroid(project: Project): AppExtension? {
        return project.extensions.findByType(AppExtension::class.java)
    }

    /**
     * 获取applicationId
     */
    fun getPackageName(project: Project): String {
        return (project.extensions.getByName("android") as AndroidConfig).defaultConfig.applicationId
    }

    /**
     * 获取启动的activity
     */
    fun getBootActivity(project: Project): String {
        var bootActivity = getBootActivity(getAndroidManifestPath(project)).toString()
        if (bootActivity == "") {
            getAndroid(project)?.applicationVariants?.all { variant ->
                val variantName = variant.name.capitalize()
                val manifestPath = project.buildDir.path + "/intermediates/merged_manifests/${variantName}/AndroidManifest.xml"
                if (File(manifestPath).exists()) {
                    bootActivity = getBootActivity(manifestPath).toString()
                    if (bootActivity != "") {
                        return@all
                    }
                }
            }
        }
        return bootActivity
    }

    /**
     * 获取启动的activity
     */
    private fun getBootActivity(path: String): String? {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document: Document = builder.parse(File(path))
        val root = document.documentElement
        val nodeList = root.childNodes
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE && node.nodeName == "application") {
                for (i in 0 until node.childNodes.length) {
                    val activity = node?.childNodes?.item(i)
                    if (activity != null && activity.nodeType == Node.ELEMENT_NODE && activity.nodeName == "activity") {
                        for (i in 0 until activity.childNodes.length) {
                            val filter = activity.childNodes?.item(i)
                            if (filter != null && filter.nodeType == Node.ELEMENT_NODE && filter.nodeName == "intent-filter") {
                                var hasMainAttr = false
                                var hasLauncherAttr = false
                                for (i in 0 until filter.childNodes.length) {
                                    val action = filter.childNodes?.item(i)
                                    if (action != null && action.nodeType == Node.ELEMENT_NODE && action.nodeName == "action"
                                        && action.attributes.getNamedItem("android:name").nodeValue == "android.intent.action.MAIN"
                                    ) {
                                        hasMainAttr = true
                                    }
                                    if (action != null && action.nodeType == Node.ELEMENT_NODE && action.nodeName == "category"
                                        && action.attributes.getNamedItem("android:name").nodeValue == "android.intent.category.LAUNCHER"
                                    ) {
                                        hasLauncherAttr = true
                                    }
                                }
                                if (hasMainAttr && hasLauncherAttr) {
                                    return activity.attributes?.getNamedItem("android:name")?.nodeValue
                                }
                            }
                        }
                    }
                }
            }
        }
        return ""
    }

}