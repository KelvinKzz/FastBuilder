package com.gaoding.fastbuilder.plugin.util

import com.gaoding.fastbuilder.lib.utils.*
import org.gradle.api.Project
import java.io.File

object ProjectUtil {

    fun resOpen(project: Project) {
        //java -jar -Dfile.encoding=UTF-8 /lib/hot.jar /build res
        val cmd = ArrayList<String>()
        cmd.add("java")
//        cmd.add(BuilderInitializer.getJavaHome() + "/bin/java")
        cmd.add("-jar")
        cmd.add("-Dfile.encoding=UTF-8")
        cmd.add(BuildUtils.getBuildToolPath(project) + "/lib/hot.jar ")
        cmd.add(BuildUtils.getRootProjectPath(project) + "/build")
        cmd.add("res")
        CmdUtil.cmd(cmd)
    }

    fun installApp(project: Project, path: String) {
        //  adb install -r -t -d debug.apk
        val cmd = ArrayList<String>()
        cmd.add(BuildUtils.getAdbCmdPath(project))
        cmd.add("install")
        cmd.add("-t")
        cmd.add("-r")
        cmd.add("-d")
        cmd.add(path)
        CmdUtil.cmd(cmd)
    }

    fun restartApp(project: Project) {
        val packageName = BuildUtils.getPackageName(project)
        val bootActivity = BuildUtils.getBootActivity(project)
        val cmd = ArrayList<String>()
        cmd.add(BuildUtils.getAdbCmdPath(project))
        cmd.add("shell")
        cmd.add("am")
        cmd.add("force-stop")
        cmd.add(packageName)
        CmdUtil.cmd(cmd)
        cmd.clear()
        cmd.add(BuildUtils.getAdbCmdPath(project))
        cmd.add("shell")
        cmd.add("am")
        cmd.add("start")
        cmd.add("-n")
        cmd.add("$packageName/$bootActivity")
        CmdUtil.cmd(cmd)
    }

    //d8
    fun jar2Dex(project: Project,  jarPathTemp: String){
        var jarPath = jarPathTemp
        Log.i("jar2Dex jarPath：$jarPath")
        val file = File(jarPath)
        if (file.isDirectory) {
            val time = System.currentTimeMillis()
            val jarOut = getJarPath(project, jarPath)
            val zipPath = jarOut + File.separator + "class.jar"
            ZipUtil.zip(zipPath, jarPath)
            jarPath = zipPath
            Log.i("class zip time：" + (System.currentTimeMillis() - time))
        }

        val dexOut = getDexPath(project, jarPath)
        val cmd = ArrayList<String>()
        cmd.add(BuildUtils.getD8CmdPath(project))
        cmd.add("--debug")
        cmd.add("--min-api")
        cmd.add("26")//26解决java8特性问题
        cmd.add("--lib")
        cmd.add(BuildUtils.getAndroidJarPath(project))
        cmd.add("--output")
        cmd.add(dexOut)
        cmd.add(jarPath)
//        cmd.add("-JXms1024M")
//        cmd.add("-JXmx2048M")
        CmdUtil.cmd(cmd)
    }

    //dx
    fun jar2DexDx(project: Project, jarPath: String) {
        val dexOut = getDexPath(project, jarPath)
        val cmd = ArrayList<String>()
        cmd.add(BuildUtils.getDxCmdPath(project))
        cmd.add("--multi-dex")
        cmd.add("--dex")
        cmd.add("--min-sdk-version=26")//26解决java8特性问题
        cmd.add("--core-library")//解决定义[java.* or javax.*]包名报错
        cmd.add("--num-threads=6")
        cmd.add("--output=" + dexOut)
        cmd.add(jarPath)
        cmd.add("-JXms1024M")
        cmd.add("-JXmx2048M")
        CmdUtil.cmd(cmd)
    }

    fun getDexPath(project: Project, jarPath: String): String {
        val md5Name = MD5Util.getMd5(jarPath)
        val dexOutPath = BuildUtils.getFastBuildPath(project) + "/dex/package/" + md5Name
        FileUtil.deleteDir(File(dexOutPath))
        FileUtil.ensumeDir(File(dexOutPath))
        return dexOutPath
    }

    fun getJarPath(project: Project, jarPath: String):
            String {
        val md5Name = MD5Util.getMd5(jarPath)
        val dexOutPath = BuildUtils.getFastBuildPath(project) + "/jar/" + md5Name
        FileUtil.deleteDir(File(dexOutPath))
        FileUtil.ensumeDir(File(dexOutPath))
        return dexOutPath
    }

    fun resetApp(project: Project) {
//        adb shell  rm -r /sdcard/patch_dex.jar
//        adb shell  rm -r /sdcard/patch_resources.apk
        val cmd = ArrayList<String>()
        cmd.add(BuildUtils.getAdbCmdPath(project))
        cmd.add("shell")
        cmd.add("rm")
        cmd.add("-r")
        cmd.add(BuildUtils.getExternalCacheDir(project) + "/patch_dex.jar")
        CmdUtil.cmd(cmd)

        cmd.clear()
        cmd.add(BuildUtils.getAdbCmdPath(project))
        cmd.add("shell")
        cmd.add("rm")
        cmd.add("-r")
        cmd.add(BuildUtils.getExternalCacheDir(project) + "/patch_resources.apk")
        CmdUtil.cmd(cmd)
    }

    fun pushFile2SD(project: Project, path: String) {
        if (!FileUtil.fileExists(path)) {
            return
        }

        val cmd = ArrayList<String>()
        cmd.add(BuildUtils.getAdbCmdPath(project))
        cmd.add("push")
        cmd.add(path)
        cmd.add(BuildUtils.getExternalCacheDir(project))
        CmdUtil.cmd(cmd)
    }
}
