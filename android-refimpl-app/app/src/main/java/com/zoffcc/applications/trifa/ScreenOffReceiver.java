package com.zoffcc.applications.trifa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenOffReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))
        {
            // Re-lock the app session
            AppSessionManager.getInstance().lockApp();
        }
    }
}

