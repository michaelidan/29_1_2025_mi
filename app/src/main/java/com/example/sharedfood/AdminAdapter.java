package com.example.sharedfood;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

    private final List<Admin> adminList;
    private final RemoveAdminListener removeAdminListener;

    public AdminAdapter(List<Admin> adminList, RemoveAdminListener removeAdminListener) {
        this.adminList = adminList;
        this.removeAdminListener = removeAdminListener;
    }

    public interface RemoveAdminListener {
        void onRemove(String email);
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        Admin admin = adminList.get(position);
        holder.emailTextView.setText(admin.getEmail());

        // Hide "Remove" button for super admin
        if (admin.isSuperAdmin()) {
            holder.removeButton.setVisibility(View.GONE);
        } else {
            holder.removeButton.setVisibility(View.VISIBLE);
            holder.removeButton.setOnClickListener(v -> removeAdminListener.onRemove(admin.getEmail()));
        }
    }

    @Override
    public int getItemCount() {
        return adminList.size();
    }

    public void updateAdmins(List<Admin> admins) {
        this.adminList.clear();
        this.adminList.addAll(admins);
        notifyDataSetChanged();
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;
        Button removeButton;

        AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.adminEmailTextView);
            removeButton = itemView.findViewById(R.id.removeAdminButton);
        }
    }
}
