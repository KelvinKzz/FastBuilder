package com.gaoding.fastbuilder.hotpatch.util;

import android.util.Log;

public class LogUtil {
    public static final String TAG = "FastBuilder";
    public static void i(String s){
        Log.i(TAG, s);
    }

    public static void e(String s) {
        Log.e(TAG, s);
    }

    public static void w(String s) {
        Log.w(TAG, s);
    }
}
