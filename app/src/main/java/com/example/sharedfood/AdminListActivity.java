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

public class AdminListActivity extends AppCompatActivity {
    private static final String TAG = "AdminListActivity";
    private RecyclerView adminRecyclerView;
    private AdminAdapter adminAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set up RecyclerView
        adminRecyclerView = findViewById(R.id.adminRecyclerView);
        adminRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adminAdapter = new AdminAdapter(new ArrayList<>(), this::removeAdmin);
        adminRecyclerView.setAdapter(adminAdapter);

        // Load admin list
        loadAdmins();
    }

    private void loadAdmins() {
        db.collection("admins").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Admin> adminList = new ArrayList<>();
                task.getResult().forEach(document -> {
                    String email = document.getId();
                    boolean isSuperAdmin = email.equals("mici9578@gmail.com");
                    adminList.add(new Admin(email, isSuperAdmin));
                });
                adminAdapter.updateAdmins(adminList);
            } else {
                Log.e(TAG, "Failed to load admins", task.getException());
                Toast.makeText(this, "שגיאה בטעינת הרשימה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeAdmin(String email) {
        if (email.equals("mici9578@gmail.com")) {
            Toast.makeText(this, "לא ניתן להסיר את המנהל הראשי", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("admins").document(email)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "המנהל הוסר בהצלחה", Toast.LENGTH_SHORT).show();
                    loadAdmins(); // Reload list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing admin", e);
                    Toast.makeText(this, "שגיאה בהסרת המנהל", Toast.LENGTH_SHORT).show();
                });
    }
}
