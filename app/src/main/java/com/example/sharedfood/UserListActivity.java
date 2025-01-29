package com.example.sharedfood;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
                showTempBanDialog(user); // קריאה לדיאלוג לבחירת זמן החסימה
                break;
            case "promote":
                promoteToAdmin(user);
                break;
        }
    }
    private void showTempBanDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחר את משך החסימה")
                .setItems(new CharSequence[]{"3 שעות", "יום אחד", "3 ימים", "שבוע", "חודש"}, (dialog, which) -> {
                    long durationInHours = 0;
                    switch (which) {
                        case 0: // 3 שעות
                            durationInHours = 3;
                            break;
                        case 1: // יום אחד
                            durationInHours = 24;
                            break;
                        case 2: // 3 ימים
                            durationInHours = 72;
                            break;
                        case 3: // שבוע
                            durationInHours = 168;
                            break;
                        case 4: // חודש
                            durationInHours = 720;
                            break;
                    }
                    tempBanUser(user, durationInHours);
                })
                .setNegativeButton("ביטול", null)
                .show();
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




    private void tempBanUser(User user, long durationInHours) {
        long currentTimeMillis = System.currentTimeMillis();
        long banEndTimeMillis = currentTimeMillis + (durationInHours * 60 * 60 * 1000); // חישוב זמן פקיעת החסימה

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // עדכון המשתמש באוסף temp_banned_users
        Map<String, Object> tempBanData = new HashMap<>();
        tempBanData.put("email", user.getEmail());
        tempBanData.put("ban_end_time", banEndTimeMillis);

        db.collection("temp_banned_users")
                .document(user.getEmail())
                .set(tempBanData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "משתמש נחסם זמנית", Toast.LENGTH_SHORT).show();
                    loadUsers(); // עדכון הרשימה
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error applying temporary ban", e);
                    Toast.makeText(this, "שגיאה בחסימה זמנית", Toast.LENGTH_SHORT).show();
                });
    }


    private void checkAndRemoveExpiredTempBans() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long currentTimeMillis = System.currentTimeMillis();

        db.collection("temp_banned_users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    querySnapshot.forEach(document -> {
                        Long banEndTime = document.getLong("ban_end_time");
                        if (banEndTime != null && banEndTime < currentTimeMillis) {
                            // הסרת המשתמש מאוסף temp_banned_users
                            db.collection("temp_banned_users").document(document.getId()).delete()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "חסימה זמנית הסתיימה עבור: " + document.getId()))
                                    .addOnFailureListener(e -> Log.e(TAG, "שגיאה בהסרת חסימה זמנית עבור: " + document.getId(), e));
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "שגיאה בגישה לאוסף temp_banned_users", e));
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
