package com.example.sharedfood;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class LoginActivity extends AppCompatActivity {

    // משתנה לניהול אימות משתמשים באמצעות Firebase
    private FirebaseAuth mAuth;

    // שדות להזנת אימייל וסיסמה
    private EditText emailEditText, passwordEditText;

    // כפתור התחברות
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // קביעת קובץ ה-XML שמגדיר את התצוגה של הפעילות
        setContentView(R.layout.activity_login);

        // אתחול Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // חיבור השדות הרלוונטיים מה-XML
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        // הגדרת אירוע לחיצה על כפתור ההתחברות
        loginButton.setOnClickListener(v -> {
            // קבלת ערכי האימייל והסיסמה שהוזנו על ידי המשתמש
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // בדיקה אם אחד מהשדות ריקים
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return; // יציאה מהפונקציה אם השדות ריקים
            }

            // **בדיקת חסימה לפני ניסיון התחברות!**
            isEmailBanned(email, () -> {
                // אם המשתמש **לא חסום**, נמשיך לניסיון ההתחברות
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // התחברות הצליחה, מקבלים את המשתמש הנוכחי
                                FirebaseUser user = mAuth.getCurrentUser();
                                checkIfUserIsBannedOrAdmin(user);
                            } else {
                                // התחברות נכשלה, הצגת הודעה עם השגיאה
                                Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        });
    }

    // פונקציה לבדוק האם האימייל חסום לפני שמנסים להתחבר
    private void isEmailBanned(String email, Runnable onNotBanned) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("FirestoreDebug", "Checking if user is banned: " + email);

        // בדיקה אם המשתמש חסום לצמיתות
        db.collection("banned_users")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("FirestoreDebug", "User is permanently banned: " + email);
                        Toast.makeText(this, "Your account is permanently banned. Contact support.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("FirestoreDebug", "User is NOT permanently banned. Checking temp ban...");

                        // אם המשתמש לא חסום לצמיתות, נבדוק חסימה זמנית
                        db.collection("users")
                                .document(email)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        Long tempBanTime = userDoc.getLong("temp_ban_time");
                                        long currentTime = System.currentTimeMillis();

                                        if (tempBanTime != null) {
                                            Log.d("FirestoreDebug", "Temp ban time: " + tempBanTime + ", Current time: " + currentTime);
                                            if (tempBanTime > currentTime) {
                                                Log.d("FirestoreDebug", "User is temporarily banned: " + email);
                                                Toast.makeText(this, "Your account is temporarily banned. Contact support.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        } else {
                                            Log.d("FirestoreDebug", "User does not have temp_ban_time set.");
                                        }

                                        Log.d("FirestoreDebug", "User is NOT temporarily banned. Proceeding with login.");
                                        onNotBanned.run();
                                    } else {
                                        Log.d("FirestoreDebug", "User record not found in Firestore: " + email);
                                        onNotBanned.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreError", "Failed to check temporary ban status", e);
                                    Toast.makeText(this, "Failed to connect to the database. Please try again.", Toast.LENGTH_SHORT).show();
                                    onNotBanned.run();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to check permanent ban status", e);
                    Toast.makeText(this, "Failed to connect to the database. Please try again.", Toast.LENGTH_SHORT).show();
                    onNotBanned.run();
                });
    }


    // פונקציה לבדיקה אם המשתמש חסום או מנהל
    private void checkIfUserIsBannedOrAdmin(FirebaseUser user) {
        // בדיקה אם המשתמש ריק
        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        checkIfUserIsAdmin(user);
    }

    // פונקציה לבדיקה אם המשתמש הוא מנהל
    private void checkIfUserIsAdmin(FirebaseUser user) {
        MainActivity.isAdmin(user, isAdmin -> {
            if (isAdmin) {
                // הצגת הודעה מיוחדת למנהלים
                Toast.makeText(LoginActivity.this, "ברוך הבא, אדון מנהל! \n בשביל פעולות מנהלים לחץ על \"צור קשר\"", Toast.LENGTH_SHORT).show();
            }
            // מעבר למסך הבית לאחר הבדיקה
            Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
