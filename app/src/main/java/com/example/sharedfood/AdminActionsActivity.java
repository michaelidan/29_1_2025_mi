package com.example.sharedfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
//
public class AdminActionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_actions);

        // כפתור לניהול פוסטים
        Button managePostsButton = findViewById(R.id.managePostsButton);
        managePostsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActionsActivity.this, ManagePostsActivity.class);
            startActivity(intent);
        });
    }
}
