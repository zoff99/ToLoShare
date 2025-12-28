package com.zoffcc.applications.trifa;

import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseProtectedActivity extends AppCompatActivity
{
    @Override
    protected void onResume() {
        super.onResume();

        boolean initialized = PinStorageUtil.isPinSet(this);
        boolean pinRequired = PinStorageUtil.isPinRequired(this);
        boolean unlocked = AppSessionManager.getInstance().isUnlocked();

        Log.i("XXXX1", " " + initialized + " " + pinRequired + " " + unlocked);

        // Redirect if: 1. Setup never done OR 2. Setup done, PIN exists, but locked.
        if (!initialized || (pinRequired && !unlocked)) {
            Log.i("XXXX2", " " + initialized + " " + pinRequired + " " + unlocked);
            Intent intent = new Intent(this, CustomPinActivity.class);
            // Flags ensure the user can't "back" into the protected content
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }
}
