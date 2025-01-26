package com.example.sharedfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AdminContactUsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_contact_us);
//
        // אתחול הכפתור לפעולות מנהלים
        Button adminActionsButton = findViewById(R.id.adminActionsButton);
        adminActionsButton.setOnClickListener(v -> {
            // מעבר לדף "פעולות מנהלים"
            Intent intent = new Intent(AdminContactUsActivity.this, AdminActionsActivity.class);
            startActivity(intent);
        });
    }
}
