package com.emicasolutions.eventdetector;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

public class ActivateActivity extends AppCompatActivity {

    private EditText inputField;
    private Button copyButton;
    private Button verifyButton;
    private TextView uidField;
    private TextView resultLabel;
    private String uidHex;
    private String code;
    private String type = "Z";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_activate);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize UI elements
        uidField = findViewById(R.id.uidField);
        inputField = findViewById(R.id.inputField);
        copyButton = findViewById(R.id.copyButton);
        verifyButton = findViewById(R.id.verifyButton);
        resultLabel = findViewById(R.id.resultLabel);

        // Generate UID and convert to hexadecimal
        String uid = getAndroidUID();
        uidHex = convertToHex(uid);

        // Set UID to TextView
        Bundle extra = getIntent().getExtras();
        if(extra!=null){
            String t = extra.getString("type");
            if(t.equals(ChoosePriceTierAdapter.MONTHLY)){
                type = ChoosePriceTierAdapter.MONTHLY_CODE;
            }else if(t.equals(ChoosePriceTierAdapter.THREE_MONTHS)){
                type = ChoosePriceTierAdapter.THREE_MONTHS_CODE;
            }else if (t.equals(ChoosePriceTierAdapter.YEARLY)){
                type = ChoosePriceTierAdapter.YEARLY_CODE;
            }
        }
        code = type + "/" + LocalDate.now()+ "/"  + uidHex;
        uidField.setText(code);
        // Set Copy button action
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(code);
            }
        });

        // Set Verify button action
        verifyButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                verifyUID();
            }
        });
    }

    private String getAndroidUID() {
        // Generate a UUID as a placeholder for Android UID
        return UUID.randomUUID().toString();
    }

    private String convertToHex(String uid) {
        // Convert the UID to an 8-character hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (char c : uid.toCharArray()) {
            hexString.append(String.format("%02x", (int) c));
        }
        return hexString.substring(0, Math.min(hexString.length(), 8));
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("UID", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "UID copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String hashUID(String uid, String secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = uid + secretKey;
            byte[] hash = digest.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void verifyUID() {
        String inputUID = inputField.getText().toString();

        String originalUIDHash = hashUID(code, MainActivity.SECRET_KEY);
        String originalUIDHex = convertToHex(originalUIDHash);
        System.out.println(originalUIDHex);
        if (originalUIDHex.equals(inputUID)) {
            resultLabel.setText("UID is correct!");
            resultLabel.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            // Set product version to paid
            SettingsActivity.version.setProductVersion(true, type);
            // Finish this activity and proceed to the main application
            finish();
        } else {
            resultLabel.setText("UID is incorrect.");
            resultLabel.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
}
