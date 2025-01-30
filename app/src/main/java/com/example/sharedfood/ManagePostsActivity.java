package com.example.sharedfood;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

    public class ManagePostsActivity extends AppCompatActivity implements MyPostsAdapter.PostDeleteListener, MyPostsAdapter.PostEditListener {

    private RecyclerView recyclerView;
    private MyPostsAdapter adapter;
    private FirebaseFirestore db;
    private TextView emptyStateText;
    private List<Post> postsList;
    private static final String TAG = "ManagePostsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_posts);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views and lists
        recyclerView = findViewById(R.id.postsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        postsList = new ArrayList<>();

        setupRecyclerView();
        loadAllPosts();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyPostsAdapter(postsList, this, this);
        recyclerView.setAdapter(adapter);
    }

        private void loadAllPosts() {
            db.collection("posts")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            postsList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    // Create a Post object from the document
                                    Post post = new Post();

                                    // Set basic fields
                                    post.setUserId(document.getString("userId"));
                                    post.setDescription(document.getString("description"));

                                    // Decode and set the image
                                    String base64Image = document.getString("imageBase64");
                                    if (base64Image != null) {
                                        Bitmap bitmap = decodeBase64ToBitmap(base64Image);
                                        post.setImageBitmap(bitmap);
                                    }

                                    // Handle filters
                                    @SuppressWarnings("unchecked")
                                    List<String> filters = (List<String>) document.get("filters");
                                    post.setFilters(filters);

                                    // Handle imageUrl
                                    String imageUrl = document.getString("imageUrl");
                                    post.setImageUrl(imageUrl);

                                    // Handle imageUri
                                    String imageUriString = document.getString("imageUri");
                                    if (imageUriString != null && !imageUriString.isEmpty()) {
                                        post.setImageUri(Uri.parse(imageUriString));
                                    }

                                    // Handle location
                                    GeoPoint geoPoint = document.getGeoPoint("location");
                                    if (geoPoint != null) {
                                        post.setLocation(geoPoint);
                                    }

                                    // Handle city
                                    String city = document.getString("city");
                                    post.setCity(city);

                                    // Set document ID
                                    post.setId(document.getId());

                                    postsList.add(post);

                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing document to Post: " + e.getMessage());
                                }
                            }

                            updateEmptyState();
                            adapter.notifyDataSetChanged();

                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                            Toast.makeText(ManagePostsActivity.this, "שגיאה בטעינת הפוסטים", Toast.LENGTH_SHORT).show();
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
                .setMessage("האם אתה בטוח שברצונך למחוק פוסט זה?")
                .setPositiveButton("מחק", (dialog, which) -> deletePost(post))
                .setNegativeButton("ביטול", null)
                .show();
    }

        private void deletePost(Post post) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (user == null) {
                Log.e("ManagePosts", "User is not logged in");
                Toast.makeText(this, "שגיאה: אין משתמש מחובר", Toast.LENGTH_SHORT).show();
                return;
            }

            // קבלת האימייל של המשתמש והורדת רווחים מיותרים
            String email = user.getEmail().trim();
            Log.d("ManagePosts", "Logged in as: " + email);

            // בדיקת אימייל באוסף admins בפיירבייס
            db.collection("admins").document(email)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Log.d("ManagePosts", "User is admin: " + email);

                            // עכשיו נוסיף בדיקה אם האימייל בפועל זהה בדיוק לאימייל באוסף admins
                            String emailFromFirestore = document.getId().trim();
                            if (!email.equals(emailFromFirestore)) {
                                Log.e("ManagePosts", "Email mismatch! Expected: " + emailFromFirestore + ", Found: " + email);
                                Toast.makeText(this, "שגיאה: האימייל לא תואם למה שבפיירבייס", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // משתמש מזוהה כמנהל, מבצע מחיקה
                            db.collection("posts")
                                    .document(post.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("ManagePosts", "Post deleted successfully: " + post.getId());
                                        Toast.makeText(this, "הפוסט נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                                        loadAllPosts(); // רענון הרשימה
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("ManagePosts", "Error deleting post", e);
                                        Toast.makeText(this, "שגיאה במחיקת הפוסט", Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            Log.e("ManagePosts", "User is NOT an admin: " + email);
                            Toast.makeText(this, "אין לך הרשאה למחוק פוסטים", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ManagePosts", "Failed to check admin status", e);
                        Toast.makeText(this, "שגיאה בבדיקת הרשאות מנהל", Toast.LENGTH_SHORT).show();
                    });
        }



    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
