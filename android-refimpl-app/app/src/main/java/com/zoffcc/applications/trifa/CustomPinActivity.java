package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CustomPinActivity extends AppCompatActivity {

    private EditText etPassword;
    private Button btnAction;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_pin);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        etPassword = findViewById(R.id.et_password);
        btnAction = findViewById(R.id.btn_unlock);
        tvTitle = findViewById(R.id.tv_title); // Ensure you have this ID in your XML

        etPassword.requestFocus();

        // If never initialized, we are in "Setup Mode"
        boolean isFirstTime = !PinStorageUtil.isPinSet(this);

        if (isFirstTime) {
            tvTitle.setText("Setup: Enter Code or leave blank");
            btnAction.setText("Finish Setup");
        }

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnAction.performClick();
                return true;
            }
            return false;
        });

        btnAction.setOnClickListener(v -> {
            String input = etPassword.getText().toString();

            if (isFirstTime) {
                // Saves PIN (or empty string) and marks INITIALIZED = true
                PinStorageUtil.savePin(this, input);
                AppSessionManager.getInstance().setUnlocked(true);

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(0, 0);

                finish();
            } else {
                // Normal Unlock Mode
                if (PinStorageUtil.checkPin(this, input)) {
                    AppSessionManager.getInstance().setUnlocked(true);

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(0, 0);

                    finish();
                } else {
                    etPassword.setError("Incorrect Code");
                }
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // If they haven't set a Code or unlocked, don't let them in
        moveTaskToBack(true);
    }
}
