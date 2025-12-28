package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CustomPinActivity extends AppCompatActivity {

    private EditText etPassword;
    private Button btnAction;
    private TextView tvTitle;
    private boolean isSettingUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_pin);

        etPassword = findViewById(R.id.et_password);
        btnAction = findViewById(R.id.btn_unlock);
        tvTitle = findViewById(R.id.tv_title); // Ensure you have this ID in your XML

        // Check if a PIN already exists
        if (!PinStorageUtil.isPinSet(this)) {
            setupFirstTimeUI();
        }

        btnAction.setOnClickListener(v -> {
            String input = etPassword.getText().toString();

            if (input.isEmpty()) {
                etPassword.setError("Field cannot be empty");
                return;
            }

            if (isSettingUp) {
                handlePinCreation(input);
            } else {
                handlePinVerification(input);
            }
        });
    }

    private void setupFirstTimeUI() {
        isSettingUp = true;
        tvTitle.setText("Set New Code");
        btnAction.setText("Save and Unlock");
    }

    private void handlePinCreation(String pin) {
        PinStorageUtil.savePin(this, pin);
        Toast.makeText(this, "Code Saved Successfully", Toast.LENGTH_SHORT).show();

        // Unlock the app session
        AppSessionManager.getInstance().setUnlocked(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }

    private void handlePinVerification(String pin) {
        if (PinStorageUtil.checkPin(this, pin)) {
            AppSessionManager.getInstance().setUnlocked(true);

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            finish();
        } else {
            etPassword.setError("Incorrect Code");
            etPassword.setText("");
            Toast.makeText(this, "Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // If they haven't set a Code or unlocked, don't let them in
        moveTaskToBack(true);
    }
}
