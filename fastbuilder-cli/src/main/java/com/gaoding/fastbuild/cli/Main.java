package com.gaoding.fastbuild.cli;

import com.gaoding.fastbuild.cli.DexUtil.DexUtils;
import com.gaoding.fastbuild.cli.utils.BuildUtils;
import com.gaoding.fastbuilder.lib.utils.CmdUtil;
import com.gaoding.fastbuilder.lib.utils.CollectUtil;
import com.gaoding.fastbuilder.lib.utils.FileUtil;
import com.gaoding.fastbuilder.lib.utils.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args != null && args.length >= 2) {
            Log.i(args[0]);
            Log.i(args[1]);
            String buildJson = args[0] + "/fastbuild/build_info.json";
            if (!new File(buildJson).exists()) {
                Log.e("buildJson: " + buildJson + "不存在，请先使用AS自带的按钮编译");
                return;
            }
            BuildUtils.initConfig(buildJson);
            switch (args[1]) {
                case "dex":
                    new DexBuilder().start();
                    break;
                case "res":
                    new ResAapt2Builder().start();
                    break;
                case "install":
                    clearPatchInMobile();
                    install();
                    restartApp();
                    break;
                case "reset":
                    clearPatchInMobile();
                    DexUtils.pushDex2SD();
                    ResAapt2Builder.pushRes2SD();
                    restartApp();
                    break;
                case "clear":
                    clearPatchInMobile();
                    restartApp();
                    break;
                case "delete":
                    delete();
                    break;
                case "restart":
                    restart();
                    break;
                case "All":
                    new AllBuilder().start();
                    break;
            }
        } else {
            Log.e("参数错误");
        }
    }

    private static void delete() {
        FileUtil.deleteDir(new File(BuildUtils.getFastBuildPath()));
        Log.i("删除完成");
    }

    public static void install() {
        //  adb install -r -t -d debug.apk
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("install");
        cmdArgs.add("-r");
        cmdArgs.add("-t");
        cmdArgs.add("-d");
        cmdArgs.add(BuildUtils.getApkPath());
        CmdUtil.cmd(cmdArgs);
    }

    public static boolean restart() {
        Log.i("----restart----");
        if (true || !checkRun()) {
            return restartApp();
        }
        // 通过广播重启
        // adb shell am broadcast -a HOT_RESTART_BROADCAST
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("shell");
        cmdArgs.add("am");
        cmdArgs.add("broadcast");
        cmdArgs.add("-a");
        cmdArgs.add("HOT_RESTART_BROADCAST");
        cmdArgs.add(BuildUtils.getPackageName());
        return CmdUtil.cmd(cmdArgs);
    }

    /**
     * 检测应用是否正在运行
     * @return
     */
    public static boolean checkRun() {
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("shell");
        cmdArgs.add("pidof");
        cmdArgs.add(BuildUtils.getPackageName());
        List<String> re = CmdUtil.cmd2(CmdUtil.getCmd(cmdArgs));
        if (!CollectUtil.isEmpty(re)) {
            try {
                if (Long.parseLong(re.get(0).trim()) > 0) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 重启应用
     */
    public static boolean restartApp() {
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("shell");
        cmdArgs.add("am");
        cmdArgs.add("force-stop");
        cmdArgs.add(BuildUtils.getPackageName());
        if (!CmdUtil.cmd(cmdArgs)) {
            return false;
        }
        cmdArgs.clear();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("shell");
        cmdArgs.add("am");
        cmdArgs.add("start");
        cmdArgs.add("-n");
        cmdArgs.add(BuildUtils.getPackageName() + "/" + BuildUtils.getConfigEntity().boot_activity);
        return CmdUtil.cmd(cmdArgs);
    }

    /**
     * 清除手机上的patch文件
     */
    public static void clearPatchInMobile() {
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("shell");
        cmdArgs.add("rm");
        cmdArgs.add("-r");
        cmdArgs.add(BuildUtils.getExternalCacheDir() + "/patch_dex.jar");
        CmdUtil.cmd(cmdArgs);
        cmdArgs.clear();
        cmdArgs.add(BuildUtils.getAdbCmdPath());
        cmdArgs.add("shell");
        cmdArgs.add("rm");
        cmdArgs.add("-r");
        cmdArgs.add(BuildUtils.getExternalCacheDir() + "/patch_resources.apk");
        CmdUtil.cmd(cmdArgs);
        CmdUtil.cmd(cmdArgs);
    }


}
