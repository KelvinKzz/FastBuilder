package com.gaoding.fastbuild.cli;


import com.gaoding.fastbuild.cli.utils.Aapt2ValueHelper;
import com.gaoding.fastbuild.cli.utils.BuildUtils;
import com.gaoding.fastbuilder.lib.utils.CmdUtil;
import com.gaoding.fastbuilder.lib.utils.FileScanHelper;
import com.gaoding.fastbuilder.lib.utils.FileUtil;
import com.gaoding.fastbuilder.lib.utils.Log;
import com.gaoding.fastbuilder.lib.utils.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 使用aapt2，实现增量编译
 */
public class ResAapt2Builder extends BaseBuilder {

    public static void main(String[] args) throws Exception {
        BuildUtils.initConfig();
        new ResAapt2Builder().start();
    }

    public ResAapt2Builder() {
        super(BuildUtils.getResInfoPath(), BuildUtils.getScanResPathList());
    }


    public void start(OnBuildListener listener) throws Exception {
        long start = System.currentTimeMillis();
        Log.i("开始执行res");
        File resourcePatchPath = new File(BuildUtils.getResourcesPath() + "/patch_resources.apk");
        if (resourcePatchPath.exists()) {
            resourcePatchPath.delete();
        }
        preStart();
        if (mCompileList.isEmpty()) {
            Log.i("无修改资源");
            if (listener != null) {
                listener.onBuildFinish(OnBuildListener.BUILD_TYPE_RES, false);
            }
            return;
        }
        decodeRes();
        aapt2Compile(mCompileList);

        aapt2LinkAll();

        Log.i("Res编译完成时间：" + (System.currentTimeMillis() - start) / 1000 + "秒");
        pushRes2SD();
        if (listener != null) {
            listener.onBuildFinish(OnBuildListener.BUILD_TYPE_RES, true);
        } else {
            Main.restart();
        }
        compileRJava();
    }

    public void start() throws Exception {
        start(null);
    }

    private void aapt2CompileAll() {
        String out = BuildUtils.getResourcesPath() + "/apk/aapt2build";
        String outZip = out + "/resources.zip";
        String outRes = out + "/res";

        FileUtil.ensumeDir(new File(out));

        List<String> cmd = new ArrayList<>();
        cmd.add(BuildUtils.getAapt2CmdPath());
        cmd.add("compile");
        cmd.add("-o");
        cmd.add(outZip);
        cmd.add("--dir");
        cmd.add(BuildUtils.getResourcesPath() + "/apk/res");
        CmdUtil.cmd(cmd);

        ZipUtil.unZip(outZip, outRes);
    }

    //https://developer.android.google.cn/studio/command-line/aapt2?hl=zh_cn
    private void aapt2Compile(Set<String> list) {
        String out = BuildUtils.getResourcesPath() + "/apk/aapt2build";
        String outRes = out + "/res";

        FileUtil.ensumeDir(new File(outRes));

        List<String> cmd = new ArrayList<>();
        cmd.add(BuildUtils.getAapt2CmdPath());
        cmd.add("compile");
        cmd.add("-o");
        cmd.add(outRes);

        Aapt2ValueHelper helper = new Aapt2ValueHelper();
        for (String path : list) {
            if (path.contains("/res/values/") && path.endsWith(".xml")) {//去除重复value
                helper.parserXml(path);
//            } else if (path.contains("res/layout/") && path.endsWith(".xml")){
//                try {
//                    helper.parserXmlLayout(path);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            } else {
                cmd.add(path);
            }
        }
        helper.changeXml();
        cmd.addAll(helper.getPathList());
        CmdUtil.cmd(cmd);
    }

    private void aapt2LinkAll() {
        Aapt2ValueHelper.checkAndGeneratePublicTxt();

        String out = BuildUtils.getResourcesPath() + "/apk/aapt2build";
        String outZip = out + "/resources_new.zip";
        String outRes = out + "/res";
        String outApk = BuildUtils.getResourcesPath() + "/patch_resources.apk";

        ZipUtil.zip(outZip, outRes);

        List<String> cmd = new ArrayList<>();
        cmd.add(BuildUtils.getAapt2CmdPath());

        cmd.add("link");
        cmd.add("--rename-manifest-package");
        cmd.add(BuildUtils.getPackageName());
        cmd.add("--extra-packages");
        cmd.add(BuildUtils.getPackageName());
        cmd.add("-o");
        cmd.add(outApk);
        cmd.add("--manifest");
        cmd.add(BuildUtils.getBuildToolPath() + "/AndroidManifest.xml");
//        cmd.add("-v");
        cmd.add("-I");
        cmd.add(BuildUtils.getAndroidJarPath());
        cmd.add(outZip);
        cmd.add("--java");
        cmd.add(BuildUtils.getResourcesJavaPath());
        //如果有Assets文件夹
        String assetsPath = BuildUtils.getResourcesPath() + "/apk/assets";
        if (new File(assetsPath).exists()) {
            cmd.add("-A");
            cmd.add(assetsPath);
        }
        if (new File(BuildUtils.getResourcesPath() + "/public.txt").exists()) {
            cmd.add("--stable-ids");
            cmd.add(BuildUtils.getResourcesPath() + "/public.txt");
        }
        CmdUtil.cmd(cmd);
    }

    private void decodeRes() throws Exception {
        String out = BuildUtils.getResourcesPath() + "/apk";
        if (FileUtil.fileExists(out + "/AndroidManifest.xml")) {
            Log.i("不需要解压资源");
            return;
        }

        String apkPath = BuildUtils.getBuildApk();

        //        java - jar apktools.jar d -f - s % DIRNAME % -o % filePath %
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-jar -Dfile.encoding=UTF-8");
        cmd.add(BuildUtils.getApkToolJar());
//        cmd.add("apktool");
        cmd.add("d");
        cmd.add("-f");
        cmd.add("-s");
        cmd.add(apkPath);
        cmd.add("-o");
        cmd.add(out);
        if (!CmdUtil.cmd(cmd)) {
            throw new Exception("decodeRes error");
        }

        aapt2CompileAll();
    }


    public static void pushRes2SD() throws Exception {
        String dexPath = BuildUtils.getResourcesPath() + "/patch_resources.apk";
        if (!new File(dexPath).exists()) {
            throw new Exception("dexPath: " + dexPath + " not exist");
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(BuildUtils.getAdbCmdPath());
        cmd.add("push");
        cmd.add(dexPath);
        cmd.add(BuildUtils.getExternalCacheDir());

        CmdUtil.cmd(cmd);
    }

    public static void compileRJava() {
        List<String> cmd = new ArrayList<>();
//        cmd.add(BuildUtils.getJavacCmdPath());
        cmd.add("javac");
        cmd.add("-encoding");
        cmd.add("UTF-8");
        cmd.add("-g");
        cmd.add("-target");
        cmd.add("1.8");
        cmd.add("-source");
        cmd.add("1.8");


        String destPath = BuildUtils.getResourcesClassesPath();
        FileUtil.deleteDir(new File(destPath));
        FileUtil.ensumeDir(new File(destPath));
        cmd.add("-d");
        cmd.add(destPath);

        String javaPath = BuildUtils.getResourcesJavaPath();
        String src_list_txt = BuildUtils.getResourcesPath() + "/src_list.txt";
        FileScanHelper helper = new FileScanHelper();
        helper.scan(javaPath);
        List<String> pathStringList = new ArrayList<>();
        for (FileScanHelper.FileInfo fileInfo : helper.pathList) {
            pathStringList.add(fileInfo.path);
        }
        FileUtil.writeFile(pathStringList, src_list_txt);
        cmd.add("@" + src_list_txt);

        CmdUtil.cmd(cmd);
    }
}
