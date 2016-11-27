package com.develop.rth.gragwithflowlayout;

import android.util.Log;

/**
 * Created by rth on 16/11/15.
 */
public class DebugUtil {

    private static final String LOG_TAG = "AssembleEssay";

    public static final boolean DEBUG = true;

    public static void debug(String message) {
        if (DEBUG) {
            Log.d(LOG_TAG, message);
        }
    }

    public static void debugFormat(String msg, Object... args) {
        if (DEBUG) {
            String message = String.format(msg, args);
            Log.d(LOG_TAG, message);
        }
    }
}
