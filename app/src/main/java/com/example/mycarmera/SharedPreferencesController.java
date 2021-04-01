package com.example.mycarmera;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesController {
    private static final String SP_FILE_NAME = "msp";
    private static SharedPreferences mSp;

    public SharedPreferencesController(Context context) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        }
    }

    //单例模式
    private static SharedPreferencesController instance = null;

    public synchronized static SharedPreferencesController getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesController(context.getApplicationContext());
        }
        return instance;
    }


    public static void spPutString(String key, String value) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String spGetString(String key) {
        String value = mSp.getString(key, "");
        return value;
    }

    public static void spPutBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean spGetBoolean(String key) {
        boolean value = mSp.getBoolean(key, false);
        return value;
    }

}
