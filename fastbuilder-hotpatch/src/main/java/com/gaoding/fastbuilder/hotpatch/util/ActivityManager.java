package com.gaoding.fastbuilder.hotpatch.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.gaoding.fastbuilder.hotpatch.server.MiddlewareActivity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by xianying on 16/3/16.
 */
public class ActivityManager {

    private static final String TAG = "Freeline.ActManager";

    public static final int ACTIVITY_NONE = 0;
    public static final int ACTIVITY_CREATED = 1;
    public static final int ACTIVITY_STARTED = 2;
    public static final int ACTIVITY_RESUMED = 3;

    private static final WeakHashMap<Activity, Integer> sActivitiesRefs = new WeakHashMap();

    private static long sFirstTaskId = 0L;

    public static boolean restart(final Context context, boolean confirm) {
        Activity top = getTopActivity();
        if(top instanceof MiddlewareActivity) {
            ((MiddlewareActivity)top).reset();
            return true;
        } else {
            try {
                Intent e = new Intent(context, MiddlewareActivity.class);
                e.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                e.putExtra("reset", confirm);
                context.startActivity(e);
                return true;
            } catch (Exception exception) {
                final String str = "Fail to increment build, make sure you have <Activity android:name=\"" + MiddlewareActivity.class.getName() + "\"/> registered in AndroidManifest.xml";
                Log.e(TAG, str);
                (new Handler(Looper.getMainLooper())).post(new Runnable() {
                    public void run() {
                        Toast.makeText(context, str, Toast.LENGTH_LONG).show();
                    }
                });
                return false;
            }
        }
    }

    public static Activity getTopActivity() {
        Activity r = null;
        for (Map.Entry<Activity, Integer> e : sActivitiesRefs.entrySet()) {
            Activity a = e.getKey();
            if (a != null && e.getValue().intValue() == ACTIVITY_RESUMED) {
                r = a;
            }
        }
        return r;
    }




}
