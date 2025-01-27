package com.example.sharedfood;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import com.example.sharedfood.User;

public class UserListActivity extends AppCompatActivity {
    private static final String TAG = "UserListActivity";
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set up RecyclerView
        userRecyclerView = findViewById(R.id.userRecyclerView);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(new ArrayList<>(), this::performActionOnUser);
        userRecyclerView.setAdapter(userAdapter);

        // Load user list
        loadUsers();
    }

    private void loadUsers() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<User> userList = new ArrayList<>();
                task.getResult().forEach(document -> {
                    String email = document.getId();
                    boolean isBanned = document.getBoolean("is_banned") != null && document.getBoolean("is_banned");
                    Long tempBanTime = document.contains("temp_ban_time") ? document.getLong("temp_ban_time") : null;
                    userList.add(new User(email, isBanned, tempBanTime));
                });
                userAdapter.updateUsers(userList);
            } else {
                Log.e(TAG, "Failed to load users", task.getException());
                Toast.makeText(this, "שגיאה בטעינת רשימת המשתמשים", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void performActionOnUser(User user, String action) {
        switch (action) {
            case "ban":
                banUser(user);
                break;
            case "temp_ban":
                tempBanUser(user);
                break;
            case "promote":
                promoteToAdmin(user);
                break;
        }
    }

    private void banUser(User user) {
        db.collection("users").document(user.getEmail())
                .update("is_banned", !user.isBanned())
                .addOnSuccessListener(aVoid -> {
                    String message = user.isBanned() ? "החסימה בוטלה בהצלחה" : "המשתמש נחסם לצמיתות";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadUsers(); // Reload list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating ban status", e);
                    Toast.makeText(this, "שגיאה בעדכון הסטטוס", Toast.LENGTH_SHORT).show();
                });
    }

    private void tempBanUser(User user) {
        // Implement UI to select ban duration and update Firestore
        // Placeholder for now
        Toast.makeText(this, "חסימה זמנית תתוסף בקרוב", Toast.LENGTH_SHORT).show();
    }

    private void promoteToAdmin(User user) {
        db.collection("admins").document(user.getEmail())
                .set(new Object()) // Create admin entry
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "המשתמש הועלה לדרגת מנהל", Toast.LENGTH_SHORT).show();
                    loadUsers(); // Reload list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error promoting user to admin", e);
                    Toast.makeText(this, "שגיאה בהפיכת המשתמש למנהל", Toast.LENGTH_SHORT).show();
                });
    }
}
