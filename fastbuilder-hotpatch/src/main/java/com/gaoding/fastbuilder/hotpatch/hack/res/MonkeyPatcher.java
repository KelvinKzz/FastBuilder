/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gaoding.fastbuilder.hotpatch.hack.res;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.gaoding.fastbuilder.hotpatch.util.LogUtil;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * A utility class which uses reflection hacks to replace the application instance and
 * the resource data for the current app.
 * This is based on the reflection parts of
 * com.google.devtools.build.android.incrementaldeployment.StubApplication,
 * plus changes to compile on JDK 6.
 * <p>
 * It now also has a lot of extra reflection machinery to do live resource swapping
 * in a running app (e.g. swiping through data structures, updating resource managers,
 * flushing cached theme entries, etc.)
 * <p>
 * The original is
 * https://github.com/google/bazel/blob/master/src/tools/android/java/com/google/devtools/build/android/incrementaldeployment/StubApplication.java
 * (May 11 revision, ca96e11)
 * <p>
 * (The code to handle resource loading etc is different; see FileManager.)
 * Furthermore, the resource patching was hacked on some more such that it can
 * handle live (activity-restart) changes, which allows us to for example patch
 * the theme and have existing activities have their themes updated!
 * <p>
 * Original comment for the StubApplication, which contained the reflection methods:
 * <p>
 * A stub application that patches the class loader, then replaces itself with the real application
 * by applying a liberal amount of reflection on Android internals.
 * <p>
 * <p>This is, of course, terribly error-prone. Most of this code was tested with API versions
 * 8, 10, 14, 15, 16, 17, 18, 19 and 21 on the Android emulator, a Nexus 5 running Lollipop LRX22C
 * and a Samsung GT-I5800 running Froyo XWJPE. The exception is {@code monkeyPatchAssetManagers},
 * which only works on Kitkat and Lollipop.
 * <p>
 * <p>Note that due to a bug in Dalvik, this only works on Kitkat if ART is the Java runtime.
 * <p>
 * <p>Unfortunately, if this does not work, we don't have a fallback mechanism: as soon as we
 * build the APK with this class as the Application, we are committed to going through with it.
 * <p>
 * <p>This class should use as few other classes as possible before the class loader is patched
 * because any class loaded before it cannot be incrementally deployed.
 */
public class MonkeyPatcher {

    protected static final String LOG_TAG = "Freeline.ResPatcher";

    @SuppressWarnings("unchecked")  // Lots of conversions with generic types
    public static void monkeyPatchApplication(Context context,
                                              String externalResourceFile) {

        // BootstrapApplication is created by reflection in Application#handleBindApplication() ->
        // LoadedApk#makeApplication(), and its return value is used to set the Application field in all
        // sorts of Android internals.
        //
        // Fortunately, Application#onCreate() is called quite soon after, so what we do is monkey
        // patch in the real Application instance in BootstrapApplication#onCreate().
        //
        // A few places directly use the created Application instance (as opposed to the fields it is
        // eventually stored in). Fortunately, it's easy to forward those to the actual real
        // Application class.
        try {
            // Find the ActivityThread instance for the current thread
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object currentActivityThread = getActivityThread(context, activityThread);
            // Find the mInitialApplication field of the ActivityThread to the real application
            Field mInitialApplication = activityThread.getDeclaredField("mInitialApplication");//字段
            mInitialApplication.setAccessible(true);
            Application initialApplication = (Application) mInitialApplication.get(currentActivityThread);
            mInitialApplication.set(currentActivityThread, initialApplication);//设置，没有意义
            // Replace all instance of the stub application in ActivityThread#mAllApplications with the
            // real one
            Field mAllApplications = activityThread.getDeclaredField("mAllApplications");
            mAllApplications.setAccessible(true);
            List<Application> allApplications = (List<Application>) mAllApplications
                    .get(currentActivityThread);
            for (int i = 0; i < allApplications.size(); i++) {
                allApplications.set(i, initialApplication);//替换所有Application
            }
            // Figure out how loaded APKs are stored.
            // API version 8 has PackageInfo, 10 has LoadedApk. 9, I don't know.
            Class<?> loadedApkClass;
            try {
                loadedApkClass = Class.forName("android.app.LoadedApk");
            } catch (ClassNotFoundException e) {
                loadedApkClass = Class.forName("android.app.ActivityThread$PackageInfo");
                LogUtil.w(Log.getStackTraceString(e));
            }
            Field mApplication = loadedApkClass.getDeclaredField("mApplication");
            mApplication.setAccessible(true);
            Field mResDir = loadedApkClass.getDeclaredField("mResDir");
            mResDir.setAccessible(true);
            // 10 doesn't have this field, 14 does. Fortunately, there are not many Honeycomb devices
            // floating around.
            Field mLoadedApk = null;
            try {
                mLoadedApk = Application.class.getDeclaredField("mLoadedApk");
            } catch (NoSuchFieldException e) {
                LogUtil.w(Log.getStackTraceString(e));
                // According to testing, it's okay to ignore this.
            }
            // Enumerate all LoadedApk (or PackageInfo) fields in ActivityThread#mPackages and
            // ActivityThread#mResourcePackages and do two things:
            //   - Replace the Application instance in its mApplication field with the real one
            //   - Replace mResDir to point to the external resource file instead of the .apk. This is
            //     used as the asset path for new Resources objects.
            //   - Set Application#mLoadedApk to the found LoadedApk instance
            for (String fieldName : new String[]{"mPackages", "mResourcePackages"}) {
                Field field = activityThread.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(currentActivityThread);
                for (Map.Entry<String, WeakReference<?>> entry :
                        ((Map<String, WeakReference<?>>) value).entrySet()) {
                    Object loadedApk = entry.getValue().get();
                    if (loadedApk == null) {
                        continue;
                    }
                    mApplication.set(loadedApk, initialApplication);
                    if (externalResourceFile != null) {
                        mResDir.set(loadedApk, externalResourceFile);
                    }

                    if (mLoadedApk != null) {
                        try {
                            mLoadedApk.set(initialApplication, loadedApk);
                        } catch (Throwable e) {
                            LogUtil.w(Log.getStackTraceString(e));
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LogUtil.w(Log.getStackTraceString(e));
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


}