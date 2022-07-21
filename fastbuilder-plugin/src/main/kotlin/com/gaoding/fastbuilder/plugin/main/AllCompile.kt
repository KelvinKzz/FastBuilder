package com.gaoding.fastbuilder.plugin.main


import com.gaoding.fastbuilder.lib.utils.FileScanHelper
import com.gaoding.fastbuilder.lib.utils.FileUtil
import com.gaoding.fastbuilder.lib.utils.Log
import com.gaoding.fastbuilder.plugin.util.BuilderInitializer
import com.gaoding.fastbuilder.plugin.util.BuildUtils
import com.gaoding.fastbuilder.plugin.util.ProjectUtil
import java.io.File

/**
 * 全量编译时，备份数据
 */
class AllCompile : BaseCompile() {

    override fun apply() {
        project.afterEvaluate {
            dexTask()
        }
    }

    private fun dexTask() {

        //缓存id aapt2
        val publicTxtPath = BuildUtils.getFastBuildResourcesPath(project) + "/public.txt"
        Log.i("--emit-ids $publicTxtPath")
        BuildUtils.getAndroid(project)?.aaptOptions?.additionalParameters("--emit-ids", publicTxtPath)

        project.tasks.findByName("preBuild")?.doLast {
            BuilderInitializer.init(project)
            val resCompile = ResCompile()
            resCompile.project = project
            resCompile.collectRes()

            scanJavaAndKotlin(BuilderInitializer.javaSet)

            scanResources(BuilderInitializer.resSet)
        }

        BuildUtils.getAndroid(project)?.applicationVariants?.all { variant ->
            val variantName = variant.name.capitalize()
            project.tasks.findByName("assemble$variantName")?.doLast {
                BuilderInitializer.updateBootActivity(project)
                BuilderInitializer.saveJson(project)
                BuilderInitializer.clear()
            }
            val packageTask = project.tasks.findByName("package$variantName")
            packageTask?.doLast {
                ProjectUtil.resetApp(project)

                val startTime = System.currentTimeMillis()
                var apkPath = ""
                for (f in packageTask.outputs.files.files) {
                    if (f != null && f.isDirectory && f.listFiles() != null) {
                        apkPath = f.listFiles().find { file -> file.absolutePath.endsWith(".apk") }?.absolutePath
                            ?: ""
                        Log.i("apkPath:$apkPath")
                    }
                }
                project.rootProject.subprojects { subProject ->
                    //避免丢失
                    val libraryClassFile = File(subProject.buildDir.path + "/intermediates/compile_library_classes_jar/debug/classes.jar")
                    if (libraryClassFile.exists()) {
                        JarClassPathManager.addClassPath(libraryClassFile.path)
                    }
                }
                JarClassPathManager.writeFile(project)
                if (apkPath != "") {
                    val apk = BuildUtils.getFastBuildPath(project) + "/apk/fastbuilder.apk"
                    FileUtil.ensumeDir(BuildUtils.getFastBuildPath(project) + "/apk")
                    FileUtil.fileCopy(apkPath, apk)
//                    ProjectUtil.installApp(project, apk)
//                    ProjectUtil.restartApp(project)
                    Log.i("打包结束：" + (System.currentTimeMillis() - startTime) / 1000)
//                    ProjectUtil.resOpen(project)
                    Log.i("=======编译完成=======")
                }
            }

        }
    }

    private fun scanJavaAndKotlin(srcPath: Set<String>) {
        val startTime = System.currentTimeMillis()
        Log.i("备份java信息:")
        val helper = FileScanHelper()
        for (path in srcPath) {
            if (FileUtil.dirExists(path)) {
                Log.i("加入扫描 $path")
                helper.scanJavaAndKotlin(path)
            }
        }
        FileScanHelper.writeFile(helper.pathList, BuildUtils.getFastBuildPath(project) + "/java_info.txt")

        Log.i("扫描java时间：" + (System.currentTimeMillis() - startTime) / 1000)
    }


    private fun scanResources(resPath: Set<String>) {
        val startTime = System.currentTimeMillis()
        Log.i("备份resources信息:")
        val helper = FileScanHelper()
        for (path in resPath) {
            if (FileUtil.dirExists(path)) {
                Log.i("加入扫描 $path")
                helper.scan(path)
            }
        }
        FileScanHelper.writeFile(helper.pathList, BuildUtils.getFastBuildPath(project) + "/resources_info.txt")

        Log.i("扫描resources时间：" + (System.currentTimeMillis() - startTime) / 1000)
    }

}
