package com.daquexian.all2download;

import android.util.Log;

/**
 * Created by daquexian on 16-12-7.
 * Utilities for log
 */

public class LogUtil {
    public static void d(String tag, String message) {
        //if (BuildConfig.DEBUG) Log.d(tag, message);
        Log.d(tag, message);
    }
    public static void w(String tag, String message) {
        if (BuildConfig.DEBUG) Log.w(tag, message);
    }
    public static void e(String tag, String message) {
        if (BuildConfig.DEBUG) Log.e(tag, message);
    }
}
