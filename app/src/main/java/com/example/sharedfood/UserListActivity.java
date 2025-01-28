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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // שליפת רשימת המנהלים
        db.collection("admins").get().addOnCompleteListener(adminsTask -> {
            if (adminsTask.isSuccessful()) {
                List<String> adminEmails = new ArrayList<>();
                adminsTask.getResult().forEach(admin -> adminEmails.add(admin.getId()));

                // שליפת כל המשתמשים שאינם מנהלים
                db.collection("users").get().addOnCompleteListener(usersTask -> {
                    if (usersTask.isSuccessful()) {
                        List<User> userList = new ArrayList<>();
                        usersTask.getResult().forEach(document -> {
                            String email = document.getId();
                            if (!adminEmails.contains(email)) { // רק אם המשתמש אינו מנהל
                                boolean isBanned = document.getBoolean("is_banned") != null && document.getBoolean("is_banned");
                                Long tempBanTime = document.contains("temp_ban_time") ? document.getLong("temp_ban_time") : null;
                                userList.add(new User(email, isBanned, tempBanTime));
                            }
                        });
                        userAdapter.updateUsers(userList); // עדכון רשימת המשתמשים בתצוגה
                    } else {
                        Log.e(TAG, "Failed to load users", usersTask.getException());
                    }
                });
            } else {
                Log.e(TAG, "Failed to load admins", adminsTask.getException());
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
        boolean isBanned = user.isBanned(); // האם המשתמש כרגע חסום
        db.collection("users").document(user.getEmail())
                .update("is_banned", !isBanned) // עדכון שדה is_banned
                .addOnSuccessListener(aVoid -> {
                    if (isBanned) {
                        // אם המשתמש היה חסום - ביטול חסימה
                        db.collection("banned_users").document(user.getEmail())
                                .delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(this, "החסימה בוטלה בהצלחה", Toast.LENGTH_SHORT).show();
                                    loadUsers(); // עדכון רשימה
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error removing user from banned_users", e);
                                    Toast.makeText(this, "שגיאה בהסרת המשתמש מהאוסף banned_users", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // אם המשתמש לא היה חסום - הוספת חסימה
                        Map<String, Object> bannedData = new HashMap<>();
                        bannedData.put("email", user.getEmail());
                        bannedData.put("banned_at", System.currentTimeMillis()); // זמן החסימה

                        db.collection("banned_users").document(user.getEmail())
                                .set(bannedData)
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(this, "המשתמש נחסם בהצלחה", Toast.LENGTH_SHORT).show();
                                    loadUsers(); // עדכון רשימה
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error adding user to banned_users", e);
                                    Toast.makeText(this, "שגיאה בהוספת המשתמש לאוסף banned_users", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user ban status", e);
                    Toast.makeText(this, "שגיאה בעדכון הסטטוס של המשתמש", Toast.LENGTH_SHORT).show();
                });
    }




    private void tempBanUser(User user) {
        // Implement UI to select ban duration and update Firestore
        // Placeholder for now
        Toast.makeText(this, "חסימה זמנית תתוסף בקרוב", Toast.LENGTH_SHORT).show();
    }

    private void promoteToAdmin(User user) {
        Log.d(TAG, "promoteToAdmin: Trying to promote " + user.getEmail()); // לצורך בדיקה

        // Prepare admin data
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("email", user.getEmail());
        adminData.put("isSuperAdmin", false); // Adjust this based on your logic

        // Add user to the 'admins' collection
        db.collection("admins").document(user.getEmail())
                .set(adminData) // Correctly serialize the admin data
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "המשתמש הועלה לדרגת מנהל", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "promoteToAdmin: Success for " + user.getEmail()); // לצורך בדיקה
                    loadUsers(); // Reload list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "promoteToAdmin: Error for " + user.getEmail(), e); // לצורך בדיקה
                    Toast.makeText(this, "שגיאה בהפיכת המשתמש למנהל", Toast.LENGTH_SHORT).show();
                });
    }

}
