package com.zoffcc.applications.trifa;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseProtectedActivity extends AppCompatActivity
{
    @Override
    protected void onResume() {
        super.onResume();
        // This check runs every time the activity comes to the foreground
        if (!AppSessionManager.getInstance().isUnlocked()) {
            Intent intent = new Intent(this, CustomPinActivity.class);
            // Flags ensure the user can't "back" into the protected content
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
