package com.zoffcc.applications.trifa;

public class friend_locations
{
    public static boolean isRunning = false;
    public static void onPause()
    {
        isRunning = false;
    }

    public static void onResume()
    {
        isRunning = true;
    }
}
