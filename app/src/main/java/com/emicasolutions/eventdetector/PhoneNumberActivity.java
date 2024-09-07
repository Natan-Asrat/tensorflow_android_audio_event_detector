package com.emicasolutions.eventdetector;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PhoneNumberActivity extends AppCompatActivity {

    private EditText phoneNumberEditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_phone_number);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber);
        saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = phoneNumberEditText.getText().toString();

                if (phoneNumber.isEmpty()) {
                    Toast.makeText(PhoneNumberActivity.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                } else {
                    // Save the phone number in SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("phoneNumber", phoneNumber);
                    editor.apply();

                    Toast.makeText(PhoneNumberActivity.this, "Phone number saved", Toast.LENGTH_SHORT).show();

                    // Navigate to MainActivity
                    Intent intent = new Intent(PhoneNumberActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Close PhoneNumberActivity
                }
            }
        });
    }
}
