package com.gaoding.fastbuilder.hotpatch.hack.res;

import android.util.Log;


import com.gaoding.fastbuilder.hotpatch.hack.HotPatchApplication;
import com.gaoding.fastbuilder.hotpatch.util.LogUtil;

import java.io.File;

public class GradleDynamic {

    public static boolean applyDynamicRes() {
        File apkFile = new File(HotPatchApplication.getContext().getExternalCacheDir(), "patch_resources.apk");
        if (!apkFile.exists()) {
            return false;
        }
        LogUtil.i("dynamicResPath: " + apkFile.getAbsolutePath());
        try {
            MonkeyPatcher.monkeyPatchApplication(HotPatchApplication.getContext(), apkFile.getAbsolutePath());//freeline
            LogUtil.i("GradleDynamic apply dynamic resource successfully");
        } catch (Throwable throwable) {
            LogUtil.e(Log.getStackTraceString(throwable));
        }
        return true;
    }

}
