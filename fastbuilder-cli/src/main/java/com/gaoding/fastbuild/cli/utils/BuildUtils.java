package com.gaoding.fastbuild.cli.utils;

import com.gaoding.fastbuild.cli.config.BuilderConfig;
import com.gaoding.fastbuild.cli.config.Config;
import com.gaoding.fastbuilder.lib.utils.FileUtil;
import com.gaoding.fastbuilder.lib.utils.Log;
import com.gaoding.fastbuilder.lib.utils.StringUtil;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;


public class BuildUtils {

    private static Config sConfig;

    public static Config getConfigEntity() {
        return sConfig;
    }

//    private static final String DEFAULT_KOTLINC_PATH = "/Applications/Android Studio.app/Contents/plugins/Kotlin/kotlinc";

    public static void initConfig() {
        String path = "./build/fastbuild/build_info.json";
        initConfig(path);
    }

    public static void initConfig(String path) {
        Gson gson = new Gson();
        String json = FileUtil.readContents(path);
        sConfig = gson.fromJson(json, Config.class);

//        if (!new File(DEFAULT_KOTLINC_PATH).exists()) {
            String bJson = FileUtil.readContents(getBuildToolPath() + "/config.json");
            if (bJson != null) {
                BuilderConfig sBuilderConfig = gson.fromJson(bJson, BuilderConfig.class);
                if (sBuilderConfig.kotlin_path != null) {
                    sConfig.kotlin_home = sBuilderConfig.kotlin_path;
                }
            }
//        } else {
//            sConfig.kotlin_home = DEFAULT_KOTLINC_PATH.replace(" ", "\\ ");
//        }
    }

    public static String getKotlinPath() {
        return sConfig.kotlin_home;
    }

    public static String getFastBuildPath() {
        return sConfig.out_dir;
    }

    public static String getBuildToolPath() {
        return sConfig.build_tool_dir;
    }

    public static String getApkToolJar() {
        return getBuildToolPath() + "/lib/apktool-cli-all.jar";
    }

    public static String getBuildApk() {
        return BuildUtils.getFastBuildPath() + "/apk/fastbuilder.apk";
    }

    public static String getResourcesPath() {
        return BuildUtils.getFastBuildPath() + "/resources";
    }

    public static String getResourcesJavaPath() {
        return BuildUtils.getResourcesPath() + "/inr_java";
    }

    public static String getResourcesClassesPath() {
        return BuildUtils.getResourcesPath() + "/inr_classe";
    }

    public static String getPackageName() {
        return sConfig.package_name_manifest;
    }

    public static String getAndroidJarPath() {
        return sConfig.compile_sdk_directory + "/android.jar";
    }

    public static String getD8CmdPath() {
        return sConfig.build_tools_directory + "/d8";
    }


    public static String getAaptCmdPath() {
        return sConfig.build_tools_directory + "/aapt";
    }

    public static String getAapt2CmdPath() {
        return sConfig.build_tools_directory + "/aapt2";
    }

    public static String getAdbCmdPath() {
        return sConfig.sdk_directory + "/platform-tools/adb";
    }

    public static List<String> getScanPathList() {
        return sConfig.scan_src;
    }

    public static List<String> getScanResPathList() {
        return sConfig.scan_res;
    }

    public static List<String> getSuperClassList() {
        return FileUtil.getStrings(getSuperClassListPath());
    }

    public static String getApkPath() {
        return sConfig.out_dir + "/apk/fastbuilder.apk.apk";
    }

    public static String getSuperClassListPath() {
        return sConfig.out_dir + "/super_class_list.txt";
    }

    public static List<String> getJarPathList() {
        String path = sConfig.out_dir + "/jar_list.txt";
        return FileUtil.getStrings(path);
    }

    public static String getJavaInfoPath() {
        return getFastBuildPath() + "/java_info.txt";
    }

    public static String getKotlinInfoPath() {
        return getFastBuildPath() + "/kotlin_info.txt";
    }

    public static String getResInfoPath() {
        return getFastBuildPath() + "/resources_info.txt";
    }

    public static String getExternalCacheDir(){
        return "sdcard/Android/data/" + BuildUtils.getPackageName() + "/cache";
    }
}
