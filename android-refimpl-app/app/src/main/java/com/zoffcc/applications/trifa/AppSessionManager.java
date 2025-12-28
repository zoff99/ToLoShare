package com.zoffcc.applications.trifa;

public class AppSessionManager
{
    private static AppSessionManager instance;
    private boolean isUnlocked = false;

    public static synchronized AppSessionManager getInstance() {
        if (instance == null) instance = new AppSessionManager();
        return instance;
    }

    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }

    // Call this when the screen turns off
    public void lockApp()
    {
        isUnlocked = false;
    }
}

