package com.gaoding.fastbuild.cli.DexUtil;

import com.gaoding.fastbuild.cli.utils.BuildUtils;
import com.gaoding.fastbuilder.lib.utils.CmdUtil;
import com.gaoding.fastbuilder.lib.utils.CollectUtil;
import com.gaoding.fastbuilder.lib.utils.Log;
import com.gaoding.fastbuilder.lib.utils.StringUtil;
import com.gaoding.fastbuilder.lib.utils.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @description: Created by zhisui on 2022/3/22
 * E-Mail Address: zhisui@gaoding.com
 */
public class DexUtils {

    public static boolean compileKotlin(Set<String> ktList, List<String> compileClassPath) {
        if (CollectUtil.isEmpty(ktList)) {
            return true;
        }
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add("java");
        cmdArgs.add("-cp");
        cmdArgs.add(BuildUtils.getKotlinPath() + "/lib/kotlin-compiler.jar");
//        cmdArgs.add("-Xplugin");
//        cmdArgs.add(BuildUtils.getKotlinPath() + "/lib/android-extensions-compiler.jar");
        cmdArgs.add("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler");
        cmdArgs.add("-jvm-target");
        cmdArgs.add("1.8");
        cmdArgs.add("-nowarn");
//        cmdArgs.add("-no-stdlib ");
        cmdArgs.addAll(compileClassPath);
        return CmdUtil.cmd(cmdArgs);
    }

    public static boolean compileJava(Set<String> javaList, List<String> compileClassPath){
        if (CollectUtil.isEmpty(javaList)) {
            return true;
        }
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add("javac");
        cmdArgs.add("-encoding");
        cmdArgs.add("UTF-8");
        cmdArgs.add("-g");
        cmdArgs.add("-target");
        cmdArgs.add("1.8");
        cmdArgs.add("-source");
        cmdArgs.add("1.8");
        cmdArgs.addAll(compileClassPath);
        return CmdUtil.cmd(cmdArgs);
    }

    public static boolean classes2DexUseD8() {
        Log.i("----classes2DexUseD8----");
        String jarPath = BuildUtils.getFastBuildPath() + "/dex/patch_dex_temp.jar";
        String dexOut = BuildUtils.getFastBuildPath() + "/dex/patch_dex.jar";
        String classPath = BuildUtils.getFastBuildPath() + "/dex/classes";

        File file = new File(classPath);
        if (file.isDirectory()) {
            long time = System.currentTimeMillis();
            ZipUtil.zip(jarPath, classPath);
            Log.i("class zip time：" + (System.currentTimeMillis() - time));
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(BuildUtils.getD8CmdPath());
        cmd.add("--debug");
        cmd.add("--min-api");
        cmd.add("26");//26解决java8特性问题
        cmd.add("--lib");
        cmd.add(BuildUtils.getAndroidJarPath());
        cmd.add("--output");
        cmd.add(dexOut);
        cmd.add(jarPath);
        return CmdUtil.cmd(cmd);
    }

    public static boolean pushDex2SD() throws Exception {
        Log.i("----pushDex2SD----");
        String dexPath = BuildUtils.getFastBuildPath() + "/dex/patch_dex.jar";
        if (!new File(dexPath).exists()) {
            throw new Exception("dexPath: " + dexPath + " not exist");
        }
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("push");
        cmdArgs.add(dexPath);
        cmdArgs.add(BuildUtils.getExternalCacheDir());

        return CmdUtil.cmd(cmdArgs);
    }

    public static boolean isWindow() {
        return System.getProperty("os.name").toLowerCase(Locale.US).contains("windows");
    }

    public static String joinClasspath(Set<String> collection) {
        StringBuilder sb = new StringBuilder();
        boolean window = isWindow();
        for (String s : collection) {
            if (!StringUtil.isEmpty(s) && (new File(s).exists())) {
                sb.append(s);
                if (window) {
                    sb.append(";");
                } else {
                    sb.append(":");
                }
            }
        }
        return sb.toString();
    }

}
