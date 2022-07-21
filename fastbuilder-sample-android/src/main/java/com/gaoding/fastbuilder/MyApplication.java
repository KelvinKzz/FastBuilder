package com.gaoding.fastbuilder;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.gaoding.fastbuilder.hotpatch.hack.HotPatchApplication;
import com.gaoding.fastbuilder.hotpatch.util.LogUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @description: Created by zhisui on 2022/3/18
 * E-Mail Address: zhisui@gaoding.com
 */
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        findApplication(base);
    }

    public void findApplication(Context context) {
        Class<?> activityThread = null;
        try {
            activityThread = Class.forName("android.app.ActivityThread");
            Object currentActivityThread = getActivityThread(context, activityThread);
            // Find the mInitialApplication field of the ActivityThread to the real application
            Field mInitialApplication = activityThread.getDeclaredField("mInitialApplication");//字段
            mInitialApplication.setAccessible(true);
            Application initialApplication = (Application) mInitialApplication.get(currentActivityThread);
            if (initialApplication == null) {
                Log.e("CWQ", "initialApplication == null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Object getActivityThread(Context context,
                                           Class<?> activityThread) {
        try {
            if (activityThread == null) {
                activityThread = Class.forName("android.app.ActivityThread");
            }
            Method m = activityThread.getMethod("currentActivityThread");
            m.setAccessible(true);
            Object currentActivityThread = m.invoke(null);
            if (currentActivityThread == null && context != null) {
                // In older versions of Android (prior to frameworks/base 66a017b63461a22842)
                // the currentActivityThread was built on thread locals, so we'll need to try
                // even harder
                Field mLoadedApk = context.getClass().getField("mLoadedApk");
                mLoadedApk.setAccessible(true);
                Object apk = mLoadedApk.get(context);
                Field mActivityThreadField = apk.getClass().getDeclaredField("mActivityThread");
                mActivityThreadField.setAccessible(true);
                currentActivityThread = mActivityThreadField.get(apk);
            }
            return currentActivityThread;
        } catch (Throwable e) {
            LogUtil.w(Log.getStackTraceString(e));
            return null;
        }
    }

    @Override
    public void onCreate() {
        Log.e("CWQ", "onCreate");
        findApplication(this);
        super.onCreate();
    }
}
