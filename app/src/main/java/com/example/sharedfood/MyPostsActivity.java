package com.example.sharedfood;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity implements MyPostsAdapter.PostClickListener {
    private RecyclerView recyclerView;
    private MyPostsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView emptyStateText;
    private List<Post> postsList;
    private String currentUserId;

    private static final String TAG = "MyPostsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views and lists
        recyclerView = findViewById(R.id.postsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        postsList = new ArrayList<>();

        setupRecyclerView();
        setupAddButton();
        loadUserPosts();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyPostsAdapter(postsList, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupAddButton() {
        FloatingActionButton fabAddPost = findViewById(R.id.fabAddPost);
        fabAddPost.setOnClickListener(v -> startActivity(new Intent(MyPostsActivity.this, ShareYourFoodActivity.class)));
    }

    private void loadUserPosts() {
        if (auth.getCurrentUser() == null) return;

        currentUserId = auth.getCurrentUser().getUid();

        db.collection("posts")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        postsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());

                            // המרה ל-Post
                            try {
                                Post post = document.toObject(Post.class);

                                if (post != null) {
                                    // המרת ה-URI של התמונה אם קיים
                                    String imageUriString = document.getString("imageUri");
                                    if (imageUriString != null) {
                                        post.setImageUri(Uri.parse(imageUriString));
                                    }

                                    // המרת מיקום אם קיים
                                    GeoPoint geoPoint = document.getGeoPoint("location");
                                    if (geoPoint != null) {
                                        post.setLocation(geoPoint);
                                    }

                                    // הוספת הפוסט לרשימה
                                    postsList.add(post);
                                } else {
                                    Log.w(TAG, "Failed to convert document to Post object.");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing document to Post: " + e.getMessage());
                            }
                        }

                        updateEmptyState();
                        adapter.notifyDataSetChanged();

                    } else {
                        // במקרה של שגיאה
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(MyPostsActivity.this, "שגיאה בטעינת הפוסטים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        if (postsList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEditClick(Post post) {
        Intent intent = new Intent(this, ShareYourFoodActivity.class);
        intent.putExtra("POST_TO_EDIT", post);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת פוסט")
                .setMessage("האם את/ה בטוח/ה שברצונך למחוק את הפוסט?")
                .setPositiveButton("מחק", (dialog, which) -> deletePost(post))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deletePost(Post post) {
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("posts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("description", post.getDescription())
                .get()
                .addOnSuccessListener(documents -> {
                    for (DocumentSnapshot document : documents) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(MyPostsActivity.this, "הפוסט נמחק בהצלחה", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(MyPostsActivity.this, "שגיאה במחיקת הפוסט", Toast.LENGTH_SHORT).show());
                    }
                });
    }
}
