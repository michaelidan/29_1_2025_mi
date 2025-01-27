package com.example.sharedfood;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> userList;
    private final UserActionListener listener;

    public interface UserActionListener {
        void onAction(User user, String action);
    }

    public UserAdapter(List<User> userList, UserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
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
        holder.emailTextView.setText(user.getEmail());

        // Update button states based on user status
        holder.banButton.setText(user.isBanned() ? "בטל חסימה" : "חסום");
        holder.banButton.setOnClickListener(v -> listener.onAction(user, "ban"));

        holder.tempBanButton.setOnClickListener(v -> listener.onAction(user, "temp_ban"));
        holder.promoteButton.setOnClickListener(v -> listener.onAction(user, "promote"));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateUsers(List<User> updatedList) {
        userList.clear();
        userList.addAll(updatedList);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;
        Button banButton, tempBanButton, promoteButton;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.userEmailTextView);
            banButton = itemView.findViewById(R.id.banUserButton);
            tempBanButton = itemView.findViewById(R.id.tempBanUserButton);
            promoteButton = itemView.findViewById(R.id.promoteUserButton);
        }
    }
}
