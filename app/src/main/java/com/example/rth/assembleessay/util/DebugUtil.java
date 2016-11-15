package com.example.rth.assembleessay.util;

import android.util.Log;

/**
 * Created by rth on 16/11/15.
 */
public class DebugUtil {

    private static final String LOG_TAG = "AssembleEssay";

    public static void debug(String message) {
        Log.d(LOG_TAG, message);
    }

    public static void debugFormat(String msg, Object... args) {
        String message = String.format(msg, args);
        Log.d(LOG_TAG, message);
    }
}
