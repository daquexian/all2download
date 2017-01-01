package com.daquexian.all2download;

import android.os.Environment;

/**
 * Created by daquexian on 16-12-8.
 * Constants
 */

@SuppressWarnings("WeakerAccess")
public class Constants {
    public final static String SP_NAME = "all2download";
    public final static String IS_FIRST_TIME = "isFirstTime";
    public final static String[] DIRECTORIES = new String[]{Environment.getExternalStorageDirectory().getAbsolutePath() + "/Tencent/QQfile_recv/",
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/Telegram/Telegram Documents/",
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/tencent/MicroMsg/WeChat/"};
    public final static String[] DIR_NAMES = new String[]{getString(R.string.qq), getString(R.string.wechat), getString(R.string.telegram)};

    private static String getString(int id) {
        return MyApplication.getContext().getString(id);
    }
}
