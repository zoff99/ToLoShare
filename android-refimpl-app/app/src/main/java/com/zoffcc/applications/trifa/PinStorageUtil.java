package com.zoffcc.applications.trifa;

import android.content.Context;
import android.content.SharedPreferences;

public class PinStorageUtil {
    private static final String PREF_FILE_NAME = "app_settings";
    private static final String PIN_KEY = "user_pin";
    private static final String INITIALIZED_KEY = "is_initialized";

    /**
     * Saves the PIN and marks the app as initialized.
     * If pin is "", the app is still 'initialized' but has no lock.
     */
    public static void savePin(Context context, String pin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        if (pin == null)
        {
            pin = "";
        }
        prefs.edit()
                .putString(PIN_KEY, pin)
                .putBoolean(INITIALIZED_KEY, true) // Mark setup as complete
                .commit();
    }

    public static boolean isPinSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(INITIALIZED_KEY, false);
    }

    public static boolean isPinRequired(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        String storedPin = prefs.getString(PIN_KEY, "");
        // Required only if setup is done AND the pin isn't empty
        return isPinSet(context) && !storedPin.isEmpty();
    }

    public static boolean checkPin(Context context, String inputPin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        String storedPin = prefs.getString(PIN_KEY, "");
        return storedPin.equals(inputPin);
    }
}

