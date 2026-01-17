package com.zoffcc.applications.trifa;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class MyLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "LifecycleTracker";

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "Paused: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d(TAG, "Resumed: " + activity.getLocalClassName());
    }

    // You must override all methods of the interface, even if they remain empty
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d(TAG, "Created: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d(TAG, "Started: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.d(TAG, "Stopped: " + activity.getLocalClassName());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.d(TAG, "SaveInstanceState: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "Destroyed: " + activity.getLocalClassName());
    }
}
