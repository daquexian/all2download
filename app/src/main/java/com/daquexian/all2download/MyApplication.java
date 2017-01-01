package com.daquexian.all2download;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * Created by daquexian on 16-12-8.
 * class of Application, providing Context object
 */

public class MyApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }
}
