package com.example.sharedfood;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private static final String TAG = "UserListAdapter";
    private final List<User> userList;
    private final Context context;

    public UserListAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.userEmailTextView.setText(user.getEmail());

        // Set button listeners
        holder.banUserButton.setOnClickListener(v -> handleBanUser(user));
        holder.tempBanUserButton.setOnClickListener(v -> handleTempBanUser(user));
        holder.promoteUserButton.setOnClickListener(v -> handlePromoteUser(user));

        // Update button states based on user status
        if (user.isBanned()) {
            holder.banUserButton.setText("בטל חסימה");
            holder.banUserButton.setBackgroundTintList(context.getColorStateList(R.color.green_color));
        } else {
            holder.banUserButton.setText("חסום");
            holder.banUserButton.setBackgroundTintList(context.getColorStateList(R.color.red_color));
        }

        if (user.getTempBanTime() != null && user.getTempBanTime() > System.currentTimeMillis()) {
            holder.tempBanUserButton.setText("בטל חסימה זמנית");
            holder.tempBanUserButton.setBackgroundTintList(context.getColorStateList(R.color.green_color));
        } else {
            holder.tempBanUserButton.setText("חסום זמנית");
            holder.tempBanUserButton.setBackgroundTintList(context.getColorStateList(R.color.blue_color));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void handleBanUser(User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(user.getEmail());

        if (user.isBanned()) {
            // Unban the user
            userRef.update("is_banned", false)
                    .addOnSuccessListener(aVoid -> {
                        user.setBanned(false);
                        Toast.makeText(context, "משתמש הוסר מחסימה לצמיתות", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error unbanning user", e);
                        Toast.makeText(context, "שגיאה בהסרת חסימה", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Ban the user
            userRef.update("is_banned", true)
                    .addOnSuccessListener(aVoid -> {
                        user.setBanned(true);
                        Toast.makeText(context, "משתמש נחסם לצמיתות", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error banning user", e);
                        Toast.makeText(context, "שגיאה בחסימה", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void handleTempBanUser(User user) {
        // Show dialog to select temporary ban duration
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("בחר את משך החסימה")
                .setItems(new CharSequence[]{"3 שעות", "יום אחד", "3 ימים", "שבוע", "חודש"}, (dialog, which) -> {
                    long durationInMillis = 0;
                    switch (which) {
                        case 0: // 3 שעות
                            durationInMillis = 3 * 60 * 60 * 1000;
                            break;
                        case 1: // יום אחד
                            durationInMillis = 24 * 60 * 60 * 1000;
                            break;
                        case 2: // 3 ימים
                            durationInMillis = 3 * 24 * 60 * 60 * 1000;
                            break;
                        case 3: // שבוע
                            durationInMillis = 7 * 24 * 60 * 60 * 1000;
                            break;
                        case 4: // חודש
                            durationInMillis = 30L * 24 * 60 * 60 * 1000;
                            break;
                    }

                    long tempBanTime = System.currentTimeMillis() + durationInMillis;

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users").document(user.getEmail())
                            .update("temp_ban_time", tempBanTime)
                            .addOnSuccessListener(aVoid -> {
                                user.setTempBanTime(tempBanTime);
                                Toast.makeText(context, "משתמש נחסם זמנית", Toast.LENGTH_SHORT).show();
                                notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error applying temp ban", e);
                                Toast.makeText(context, "שגיאה בחסימה זמנית", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void handlePromoteUser(User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("admins").document(user.getEmail())
                .set(new Admin(user.getEmail(), false))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "משתמש הועלה למנהל", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error promoting user", e);
                    Toast.makeText(context, "שגיאה בהעלאת משתמש למנהל", Toast.LENGTH_SHORT).show();
                });
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userEmailTextView;
        Button banUserButton;
        Button tempBanUserButton;
        Button promoteUserButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userEmailTextView = itemView.findViewById(R.id.userEmailTextView);
            banUserButton = itemView.findViewById(R.id.banUserButton);
            tempBanUserButton = itemView.findViewById(R.id.tempBanUserButton);
            promoteUserButton = itemView.findViewById(R.id.promoteUserButton);
        }
    }
}
