package com.example.sharedfood;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// ייבוא נוסף לבדיקה אם המשתמש הוא מנהל, michael %%%
import java.util.List;
import java.util.Arrays;
// סוף הייבוא הנוסף, michael %%%

public class LoginActivity extends AppCompatActivity {

    // משתנה לניהול אימות משתמשים באמצעות Firebase
    private FirebaseAuth mAuth;

    // שדות לעריכת טקסט עבור אימייל וסיסמה
    private EditText emailEditText, passwordEditText;

    // כפתור התחברות
    private Button loginButton;
    // Michael, START, 27/01/2023
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // הגדרת עיצוב המסך עבור הפעילות
        setContentView(R.layout.activity_login);

        // אתחול Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // חיבור משתני התצוגה לשדות בערכת ה-XML
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        // הגדרת מאזין לאירוע לחיצה על כפתור ההתחברות
        loginButton.setOnClickListener(v -> {
            // קבלת הטקסט מהשדות והסרת רווחים מיותרים
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // בדיקה אם השדות ריקים
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return; // יציאה מהפונקציה אם השדות ריקים
            }

            // ניסיון להתחבר באמצעות Firebase עם אימייל וסיסמה
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // התחברות הצליחה
                            FirebaseUser user = mAuth.getCurrentUser();

                            // בדיקה אם המשתמש הוא מנהל
                            checkIfUserIsAdmin(user); // michael %%%
                        } else {
                            // התחברות נכשלה, הצגת הודעת שגיאה
                            Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    //שליחת המשתמש לדף הבית, ואם הוא אדמין אז גם תציג הודעת ברוך הבא
// בדיקה אם המשתמש חסום והאם ניתן להתחבר
    private void checkIfUserIsAdmin(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getEmail().replace(".", "_")) // שימוש באימייל כ-ID
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isBanned = documentSnapshot.getBoolean("is_banned");
                        if (isBanned != null && isBanned) {
                            Toast.makeText(this, "Your account is banned. Contact support.", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut(); // Logout the user
                            finish(); // Close LoginActivity
                            return;
                        }

                        // Continue with admin check
                        MainActivity.isAdmin(user, isAdmin -> {
                            if (isAdmin) {
                                Toast.makeText(LoginActivity.this, "ברוך הבא, אדון מנהל! \n בשביל פעולות מנהלים לחץ על \"צור קשר\"", Toast.LENGTH_SHORT).show();
                            }
                            // Navigate to home page
                            Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        Toast.makeText(this, "User not found in Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check user status", Toast.LENGTH_SHORT).show();
                });
    }



    // Michael, END, 27/01/2023
}
