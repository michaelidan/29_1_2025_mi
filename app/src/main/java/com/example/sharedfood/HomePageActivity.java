
package com.example.sharedfood;


import androidx.core.app.ActivityCompat;

import androidx.core.app.NotificationCompat;

import androidx.core.app.NotificationManagerCompat;

import androidx.activity.result.contract.ActivityResultContracts;

import androidx.activity.result.ActivityResultCallback;

import android.content.pm.PackageManager;


import android.content.Intent;

import android.os.Bundle;

import android.util.Log;

import android.widget.Toast;



import androidx.appcompat.app.AppCompatActivity;



import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.QueryDocumentSnapshot;



import java.util.Arrays;

import java.util.List;


public class HomePageActivity extends AppCompatActivity {
    private FirebaseAuth mAuth; // משתנה לאחסון הפניה ל-Firebase Auth
    private FirebaseFirestore db; // משתנה לאחסון הפניה ל-Firebase Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // אתחול של Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
//
        // בדיקה ומחיקת פוסטים שפג תוקפם
        checkAndDeleteExpiredPosts();

        // כפתור "שתף אוכל"
        findViewById(R.id.shareFoodButton).setOnClickListener(v -> {
            if (checkUserLogin()) { // בדיקת האם המשתמש מחובר
                startActivity(new Intent(HomePageActivity.this, ShareYourFoodActivity.class));
            }
        });

        // כפתור "צור קשר"
        findViewById(R.id.contactUsButton).setOnClickListener(v -> {

            FirebaseUser user = mAuth.getCurrentUser();
        // Michael, 23/01/2025, START
            MainActivity.isAdmin(user, isAdmin -> {
                if (isAdmin) {
                    startActivity(new Intent(HomePageActivity.this, AdminContactUsActivity.class));
                } else {
                    startActivity(new Intent(HomePageActivity.this, activity_contact_us.class));
                }
            });
        });
        // Michael, 23/01/2025, END

        // כפתור "אזור אישי"
        findViewById(R.id.personalAreaButton).setOnClickListener(v -> {
            if (checkUserLogin()) { // בדיקת האם המשתמש מחובר
                startActivity(new Intent(HomePageActivity.this, PersonalAreaActivity.class));
            }
        });

        // כפתור "פיד"
        findViewById(R.id.feedButton).setOnClickListener(v -> {
            startActivity(new Intent(HomePageActivity.this, FeedActivity.class)); // מעבר למסך "פיד"
        });

        // כפתור "הפוסטים שלי"
        findViewById(R.id.myPostsButton).setOnClickListener(v -> {
            if (checkUserLogin()) { // בדיקת האם המשתמש מחובר
                startActivity(new Intent(HomePageActivity.this, MyPostsActivity.class));
            }
        });

        // כפתור FAB לשיתוף אוכל
        ExtendedFloatingActionButton fabShare = findViewById(R.id.fabShare);
        fabShare.setOnClickListener(v -> {
            if (checkUserLogin()) { // בדיקת האם המשתמש מחובר
                startActivity(new Intent(HomePageActivity.this, ShareYourFoodActivity.class));
            }
        });
    }

    private void checkAndDeleteExpiredPosts() {
        // שליפת כל הפוסטים מ-Firebase Firestore
        db.collection("posts").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Long timestamp = document.getLong("timestamp"); // קבלת זמן היצירה של הפוסט
                    if (timestamp != null) {
                        // חישוב זמן תפוגה על בסיס הפילטרים של הפוסט
                        Long expirationTime = calculateExpirationTime(document);
                        if (System.currentTimeMillis() > expirationTime) {
                            // מחיקת פוסט אם פג תוקף
                            db.collection("posts").document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Post deleted: " + document.getId()))
                                    .addOnFailureListener(e -> Log.e("Firebase", "Error deleting post", e));

                        }
                    }
                }
            } else {
                Log.e("Firebase", "Error getting posts: ", task.getException()); // טיפול במקרה של שגיאה בשליפת פוסטים
            }
        });
    }

    private Long calculateExpirationTime(QueryDocumentSnapshot document) {
        Long timestamp = document.getLong("timestamp"); // זמן היצירה של הפוסט
        if (timestamp == null) return Long.MAX_VALUE; // אם אין זמן יצירה, לא למחוק את הפוסט

        List<String> filters = (List<String>) document.get("filters"); // קבלת הפילטרים של הפוסט
        long expirationMillis = 0; // משתנה לשמירת זמן התפוגה

        // בדיקת קיומם של פילטרים "חם" או "קר" והגדרת זמן תפוגה מתאים
        if (filters != null && !filters.isEmpty()) {
            if (filters.contains("Hot")) {
                expirationMillis = Math.max(expirationMillis, 12 * 60 * 60 * 1000); // 12 שעות
            }
            if (filters.contains("Cold")) {
                expirationMillis = Math.max(expirationMillis, 72 * 60 * 60 * 1000); // 72 שעות
            }
        }

        // אם אין פילטרים, זמן התפוגה אינסופי
        if (expirationMillis == 0) {
            return Long.MAX_VALUE;
        }
        return timestamp + expirationMillis; // חישוב זמן התפוגה הסופי
    }

    private boolean checkUserLogin() {
        if (mAuth.getCurrentUser() == null) { // בדיקה אם המשתמש מחובר
            Toast.makeText(this, "נא להתחבר תחילה", Toast.LENGTH_SHORT).show(); // הצגת הודעה למשתמש
            startActivity(new Intent(HomePageActivity.this, LoginActivity.class)); // מעבר למסך התחברות
            return false;
        }
        return true; // המשתמש מחובר
    }
}
