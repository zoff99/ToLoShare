package com.zoffcc.applications.trifa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ChangePinActivity extends AppCompatActivity {

    private EditText etInput;
    private Button btnAction;
    private TextView tvLabel;

    private boolean isOldPinVerified = false;
    private String tempNewPin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_pin); // Reuse your existing layout

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        etInput = findViewById(R.id.et_password);
        btnAction = findViewById(R.id.btn_unlock);
        tvLabel = findViewById(R.id.tv_title);

        // Initial State: Verify identity
        if (PinStorageUtil.isPinRequired(this)) {
            tvLabel.setText("Enter Current Code");
            btnAction.setText("Verify");
        } else {
            // If no PIN was set, go straight to setting a new one
            isOldPinVerified = true;
            tvLabel.setText("Enter New Code");
            btnAction.setText("Update Code");
        }

        btnAction.setOnClickListener(v -> {
            String input = etInput.getText().toString();

            if (!isOldPinVerified) {
                handleVerifyOldPin(input);
            } else {
                handleSaveNewPin(input);
            }
        });
    }

    private void handleVerifyOldPin(String input) {
        if (PinStorageUtil.checkPin(this, input)) {
            isOldPinVerified = true;
            etInput.setText("");
            tvLabel.setText("Enter New Code (Leave blank to remove)");
            btnAction.setText("Update Code");
        } else {
            etInput.setError("Incorrect Current Code");
        }
    }

    private void handleSaveNewPin(String input) {
        // Save the new PIN (even if empty)
        PinStorageUtil.savePin(this, input);

        Toast.makeText(this, "Code Updated Successfully", Toast.LENGTH_SHORT).show();

        // Lock after setting new PIN. so the user must unlock again!
        AppSessionManager.getInstance().setUnlocked(false);
        /*
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        */
        finish();
    }
}
