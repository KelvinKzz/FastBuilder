package com.gaoding.fastbuild.cli;


import com.gaoding.fastbuild.cli.DexUtil.DexUtils;
import com.gaoding.fastbuild.cli.utils.BuildUtils;
import com.gaoding.fastbuild.cli.utils.ClassPathUtil;
import com.gaoding.fastbuilder.lib.utils.CollectUtil;
import com.gaoding.fastbuilder.lib.utils.FileScanHelper;
import com.gaoding.fastbuilder.lib.utils.FileUtil;
import com.gaoding.fastbuilder.lib.utils.Log;
import com.gaoding.fastbuilder.lib.utils.MD5Util;
import com.gaoding.fastbuilder.lib.utils.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 扫描java--》扫描缓存class--》增量编译class--》打包dex（dex/cache/classes）-->推倒sd卡--》重启app
 */
public class DexBuilder extends BaseBuilder {

    Set<String> mClassPath = new HashSet<>();

    public static void main(String[] args) throws Exception {
        BuildUtils.initConfig();
        new DexBuilder().start();
    }

    public DexBuilder() {
        super(BuildUtils.getJavaInfoPath(), BuildUtils.getScanPathList());
    }


    public void start(OnBuildListener listener) throws Exception {
        long start = System.currentTimeMillis();
        Log.i("----开始执行dex----");
        File dexPath = new File(BuildUtils.getFastBuildPath() + "/dex/patch_dex.jar");
        if (dexPath.exists()) {
            dexPath.delete();
        }
        preStart();
        if (mCompileList.isEmpty()) {
            Log.i("Dex:无修改文件");
            if (listener != null) {
                listener.onBuildFinish(OnBuildListener.BUILD_TYPE_DEX, false);
            }
            return;
        }
        compile();
        if (!DexUtils.classes2DexUseD8()) {
            throw new Exception("classes2DexUseD8 error");
        }
        Log.i("Dex:dex编译完成时间：" + (System.currentTimeMillis() - start) / 1000 + "秒");
        if (!DexUtils.pushDex2SD()) {
            throw new Exception("pushDex2SD error");
        }

        if (listener != null) {
            listener.onBuildFinish(OnBuildListener.BUILD_TYPE_DEX, true);
        } else {
            if (!Main.restart()) {
                throw new Exception("restart error");
            }
        }
    }

    public void start() throws Exception {
        start(null);
    }

    @Override
    protected void scanFile(FileScanHelper helper, String path) {
        helper.scanJavaAndKotlin(path);
    }

    public void compile() throws Exception {
        String classDestPath = BuildUtils.getFastBuildPath() + "/dex/classes";
        File classDestFile = new File(classDestPath);
        FileUtil.deleteDir(classDestFile);
        FileUtil.ensumeDir(classDestFile);

        processClassPath();

        Set<String> ktList = getList(".kt");
        if (!CollectUtil.isEmpty(ktList)) {
            Log.i("----compile kotlin----");
            //需要先编译Kotlin，classpath需要kt的class
            if (!DexUtils.compileKotlin(getList(".kt"), compileClassPath(mCompileList))) {
                Log.e("compile kotlin error");
                throw new Exception("compile kotlin error");
            }
            Log.i("compile kotlin ok");
        }
        Log.i("----compile java----");
        Set<String> javaList = getList(".java");
        if (!DexUtils.compileJava(javaList, compileClassPath(javaList))) {
            Log.e("compile java error");
            throw new Exception("compile java error");
        }
        Log.i("compile java ok");
    }

    private void processClassPath() {
        List<String> allJarList = BuildUtils.getJarPathList();//文件jar list
        mClassPath.clear();
        mClassPath.addAll(allJarList);
        Set<String> ktList = getList(".kt");
        if (CollectUtil.isEmpty(ktList)) {
            return;
        }

        String jarOutPath = BuildUtils.getFastBuildPath() + "/dex/jar/class";
        FileUtil.ensumeDir(jarOutPath);

        if (CollectUtil.isEmpty(mModifyCompileFileList)) {
            return;
        }
        //若有修改文件，则要将修改文件也加入到classpath中
        Set<String> javaList = new HashSet<>();
        Set<String> javaFileList = new HashSet<>();
        //获取修改的类
        for (FileScanHelper.FileInfo info : mModifyCompileFileList) {
            if (info.path.endsWith(".java")) {
                Log.i("path:" + info.root);
                String name = info.path.substring(info.root.length(), info.path.lastIndexOf("."));
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                javaList.add(name);
                javaFileList.add(name);
            }
        }

        if (CollectUtil.isEmpty(javaList)) {
            return;
        }

        Log.i("javaList:" + javaList);

        //解压和删除class，防止编译冲突
        for (String jar : allJarList) {
            if (!jar.startsWith(BuildUtils.getConfigEntity().root_dir)) {
                continue;
            }
            String newJar = jar;
            if (jar.endsWith(".jar") && ClassPathUtil.checkHasFile(jar, javaList)) {
                Log.i("jar remove:" + jar);
                newJar = jarOutPath + "/" + MD5Util.getMd5(jar);
                if (!FileUtil.dirExists(newJar)) {
                    ZipUtil.unZip(jar, newJar);
                }
                mClassPath.remove(jar);
                mClassPath.add(newJar);
            }
            if (FileUtil.dirExists(newJar)) {
                ClassPathUtil.fileRemoveClass(new File(newJar), javaFileList);
            }
        }
    }

    private Set<String> getList(String suffix) {
        Set<String> set = new HashSet<>();
        for (String s : mCompileList) {
            if (s.endsWith(suffix)) {
                set.add(s);
            }
        }
        return set;
    }

    public List<String> compileClassPath(Set<String> javaLit) throws IOException {

        List<String> cmdArgs = new ArrayList<>();

        Set<String> classPath = new LinkedHashSet<>();

        //输出路径
        String classDestPath = BuildUtils.getFastBuildPath() + "/dex/classes";
        classPath.add(classDestPath);

        //引用编译的jar，class
        String androidJar = BuildUtils.getAndroidJarPath();
        classPath.add(androidJar);//android.jar

        String rClassDestPath = BuildUtils.getResourcesClassesPath();
        classPath.add(rClassDestPath);//r class

        classPath.addAll(mClassPath);

        cmdArgs.add("-cp");
        cmdArgs.add(DexUtils.joinClasspath(classPath));

        //输出路径
        cmdArgs.add("-d");
        cmdArgs.add(classDestPath);

        String src_list_txt = BuildUtils.getFastBuildPath() + "/dex/src_list.txt";
        FileUtil.deleteFile(new File(src_list_txt));
        FileUtil.writeFile(javaLit, src_list_txt);
        cmdArgs.add("@" + src_list_txt);

        return cmdArgs;
    }

}
