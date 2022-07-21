package com.gaoding.fastbuilder.plugin.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.gaoding.fastbuilder.lib.utils.Log
import com.gaoding.fastbuilder.lib.utils.MultiThreadManager
import com.gaoding.fastbuilder.plugin.inject.MatrixInjector
import com.gaoding.fastbuilder.plugin.main.JarClassPathManager
import org.gradle.api.Project

/**
 * 注入代码，针对具体项目修改
 */
class InjectTransform(var project: Project) : Transform() {

    override fun getName(): String {
        return "BuilderInjectTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)
        Log.i("injectApplication Transform " + Thread.currentThread().name)
        transformInvocation?.outputProvider?.deleteAll()
        val startTime = System.currentTimeMillis();
        // inputs有两种类型，一种是目录，一种是jar，需要分别遍历。
        val manager = MultiThreadManager<String>()
        transformInvocation?.inputs?.forEach { input ->
            input.directoryInputs?.forEach { directoryInput ->
                val dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                    directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                Log.i("Transform dir path:" + directoryInput.file.absolutePath)
                JarClassPathManager.addClassPath(directoryInput.file.absolutePath)
                manager.addTask {
                    dest?.absolutePath?.let { MatrixInjector.inject(directoryInput.file, it) }.toString()
                }
            }
            input.jarInputs?.forEach { jarInput ->
                Log.i("Transform jar path:" + jarInput.file.absolutePath)
                // 重命名输出文件（同目录copyFile会冲突）
                var jarName = jarInput.name
                val md5Name = org.apache.commons.codec.digest.DigestUtils.md5Hex(jarInput.file.absolutePath)
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length - 4)
                }
                val dest = transformInvocation.outputProvider?.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                JarClassPathManager.addClassPath(jarInput.file.absolutePath)
                manager.addTask {
                    dest?.absolutePath?.let { MatrixInjector.inject(jarInput.file, it) }.toString()
                }
            }
        }
        manager.start()
        JarClassPathManager.writeFile(project)
        Log.i("transform 时间：" + (System.currentTimeMillis() - startTime) / 1000)
    }

}