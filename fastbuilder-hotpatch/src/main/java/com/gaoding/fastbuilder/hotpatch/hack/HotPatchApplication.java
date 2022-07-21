package com.gaoding.fastbuilder.hotpatch.hack;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import com.gaoding.fastbuilder.hotpatch.hack.res.GradleDynamic;
import com.gaoding.fastbuilder.hotpatch.util.ActivityManager;
import com.gaoding.fastbuilder.hotpatch.util.LogUtil;

import java.lang.reflect.Array;
import java.util.List;

public class HotPatchApplication extends Application {


    private static boolean sIsStart;

    @Override
    public void onCreate() {
        super.onCreate();

        init(this);
    }

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    public static void init(Context context) {
        if (sIsStart) {
            return;
        }
        sIsStart = true;

        mContext = context;

        LogUtil.e("Application = " + context.getClass().getName());

        try {
            //注入java
            HotPatch.dexInject();
            //注入res
            GradleDynamic.applyDynamicRes();
            //======== 以下是测试是否成功注入 =================
            Object object = HotPatch.getObject();
            int length = Array.getLength(object);
            LogUtil.e("length = " + length);
        } catch (Exception e) {
            LogUtil.w(Log.getStackTraceString(e));
        }

        registerBroadcast(mContext);
    }

    public static String getMainProcessName(Context context) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).processName;
    }

    public static boolean isMainProcess(Context context) {
        try {
            String main = getMainProcessName(context);
            String current = getCurrentProcessName(context);
            LogUtil.i("main=" + main + "， current=" + current);
            return main.equals(current);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getCurrentProcessName(Context context) {
        int pid = Process.myPid();
        String currentProcessName = "";
        android.app.ActivityManager activityManager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<android.app.ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (android.app.ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (pid == processInfo.pid) {
                currentProcessName = processInfo.processName;
            }
        }
        return currentProcessName;
    }


    private static void registerBroadcast(Context context) {
        IntentFilter intentFilter = new IntentFilter(RESTART_BROADCAST);
        context.registerReceiver(new MyReceiver(), intentFilter);
    }

    public static final String RESTART_BROADCAST = "HOT_RESTART_BROADCAST";

    public static class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (RESTART_BROADCAST.equals(intent.getAction()) && isMainProcess(context)) {
                LogUtil.i("重启");
                restart(context);
            }
        }
    }

    private static void restart(Context context) {
        ActivityManager.restart(context, true);
    }

}