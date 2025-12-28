package com.zoffcc.applications.trifa;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseProtectedActivity extends AppCompatActivity
{
    @Override
    protected void onResume() {
        super.onResume();
        // If not unlocked, redirect to the custom PIN screen
        if (!AppSessionManager.getInstance().isUnlocked()) {
            Intent intent = new Intent(this, CustomPinActivity.class);
            // Prevent user from going back to protected screen without PIN
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
