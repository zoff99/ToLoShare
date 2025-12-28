package com.zoffcc.applications.trifa;

import android.content.Context;
import android.content.SharedPreferences;

public class PinStorageUtil {

    private static final String PREF_FILE_NAME = "app_settings";
    private static final String PIN_KEY = "user_pin";

    /**
     * Stores the PIN using standard SharedPreferences.
     */
    public static void savePin(Context context, String pin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PIN_KEY, pin);
        editor.apply(); // Saves asynchronously
    }

    /**
     * Checks the entered PIN against the stored PIN.
     */
    public static boolean checkPin(Context context, String inputPin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        // Default to null if no PIN exists
        String storedPin = prefs.getString(PIN_KEY, null);

        if (storedPin == null) return false;
        return storedPin.equals(inputPin);
    }

    /**
     * Checks if a PIN has been created yet.
     */
    public static boolean isPinSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        return prefs.contains(PIN_KEY);
    }
}

