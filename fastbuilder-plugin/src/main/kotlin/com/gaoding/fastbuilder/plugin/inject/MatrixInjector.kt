package com.gaoding.fastbuilder.plugin.inject

import com.android.utils.FileUtils
import com.gaoding.fastbuilder.lib.utils.CollectUtil
import com.gaoding.fastbuilder.lib.utils.FileUtil
import com.gaoding.fastbuilder.lib.utils.Log
import com.gaoding.fastbuilder.lib.utils.StringUtil
import com.gaoding.fastbuilder.plugin.main.FastBuildPlugin
import org.objectweb.asm.Opcodes
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.*
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

object MatrixInjector {

    fun inject(file: File, out: String) {
        Log.i("inject path:" + file.absolutePath + " out:" + out)

        if (file.isDirectory) {
            realInjectClass(file, out)
        } else if (file.path.endsWith("classes.jar")) {
            realInject(file, out)
        } else {
            FileUtils.copyFile(file, File(out))
        }
    }

    //复制方法
    private fun realInjectClass(src: File, des: String) {
        //把文件里面内容放进数组
        val fs = src.listFiles()
        val file = File(des)
        //判断是否有这个文件有不管没有创建
        if (!file.exists()) {
            file.mkdirs()
        }
        //遍历文件及文件夹
        fs?.forEach { f ->
            if (f.isFile) {
                //文件
                realInject(f, des + File.separator + f.name) //调用文件拷贝的方法
            } else if (f.isDirectory) {
                //文件夹
                realInjectClass(f, des + File.separator + f.name)
                //继续调用复制方法递归的地方,自己调用自己的方法,就可以复制文件夹的文件夹了
            }
        }
    }

    private fun realInject(file: File, out: String) {
        try {
            val pending = File(out)

            if (file.path.endsWith(".class")) {
                val injectData = needInject(file.absolutePath)
                if (injectData != null) {
                    val fis = FileInputStream(file)
                    val fos = FileOutputStream(pending)
                    val bytes = hackClass(injectData, fis)
                    fos.write(bytes)
                    fis.close()
                    fos.close()
                } else {
                    FileUtil.fileCopy(file, pending)
                }

            } else if (file.path.endsWith(".jar")) {
                val jar = JarFile(file)
                val enumeration = jar.entries()
                val jos = JarOutputStream(FileOutputStream(pending))
                while (enumeration.hasMoreElements()) {
                    var ins: InputStream? = null
                    try {
                        val jarEntry = enumeration.nextElement()
                        val entryName = jarEntry.name
                        val zipEntry = ZipEntry(entryName)

                        ins = jar.getInputStream(jarEntry)
                        jos.putNextEntry(zipEntry)

                        if (entryName.endsWith(".class")) {
                            val injectData = needInject(entryName)
                            if (injectData != null) {
                                jos.write(hackClass(injectData, ins))
                            } else {
                                jos.write(readBytes(ins))
                            }
                        } else {
                            jos.write(readBytes(ins))
                        }
                    } catch (e: Exception) {
                        Log.e("inject jar with exception path： " + file.path + "\n" + Log.getStackTraceString(e))
                    } finally {
                        FileUtil.safeClose(ins)
                        jos.closeEntry()
                    }
                }
                jos.close()
                jar.close()
            }
        } catch (e: Exception) {
            Log.e("inject error path： " + file.path + "\n" + Log.getStackTraceString(e))
        }
    }


    private fun hackClass(data: InjectData, inputStream: InputStream): ByteArray {
        val classReader = ClassReader(inputStream)
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val visitor = TraceClassAdapter(Opcodes.ASM5, classWriter, data)
        classReader.accept(visitor, 0)
        return classWriter.toByteArray()
    }

    private fun readBytes(ins: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()

        var nRead: Int
        val data = ByteArray(16384)
        nRead = ins.read(data, 0, data.size)
        while (nRead != -1) {
            buffer.write(data, 0, nRead)
            nRead = ins.read(data, 0, data.size)
        }
        buffer.flush()
        return buffer.toByteArray()
    }

    private fun needInject(path: String): InjectData? {
        if (CollectUtil.isEmpty(FastBuildPlugin.builderExtension.getNameSet())) {
            return null
        }
        for (name in FastBuildPlugin.builderExtension.getNameSet()) {
            if (!StringUtil.isEmpty(name)) {
                val strings = name.split("#")
                if (strings.size == 2 && !StringUtil.isEmpty(strings[0]) && !StringUtil.isEmpty(strings[1])) {
                    if (path.replace(File.separator, ".").replace("/", ".").contains(strings[0] + ".class")) {
                        return InjectData(name, strings[1])
                    }
                }
            }
        }
        return null
    }

    class InjectData {
        var name: String = ""
        var methodName: String = ""

        constructor(name: String, methodName: String) {
            this.name = name
            this.methodName = methodName
        }
    }

}
