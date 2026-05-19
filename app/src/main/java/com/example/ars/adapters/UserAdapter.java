package com.example.ars.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.User;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private List<User> users;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onAdminToggle(User user);
        void onBanToggle(User user);
    }

    public UserAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);

        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Без email");

        var context = holder.itemView.getContext();

        if (user.getIsAdmin()) {
            holder.btnAdmin.setText("Администратор");
            int grayColor = ContextCompat.getColor(context, android.R.color.darker_gray);
            holder.btnAdmin.setBackgroundTintList(ColorStateList.valueOf(grayColor));
        } else {
            holder.btnAdmin.setText("Пользователь");
            int greenPrimary = ContextCompat.getColor(context, R.color.color_input_border);
            holder.btnAdmin.setBackgroundTintList(ColorStateList.valueOf(greenPrimary));
        }

        if (user.getInBan()) {
            holder.btnBan.setText("Разбанить");
            int safeGreen = ContextCompat.getColor(context, android.R.color.holo_green_dark);
            holder.btnBan.setBackgroundTintList(ColorStateList.valueOf(safeGreen));
        } else {
            holder.btnBan.setText("Забанить");
            int dangerRed = ContextCompat.getColor(context, android.R.color.holo_red_dark);
            holder.btnBan.setBackgroundTintList(ColorStateList.valueOf(dangerRed));
        }

        holder.btnAdmin.setOnClickListener(v -> {
            if (listener != null) listener.onAdminToggle(user);
        });

        holder.btnBan.setOnClickListener(v -> {
            if (listener != null) listener.onBanToggle(user);
        });
    }

    @Override
    public int getItemCount() { return users.size(); }

    public void updateData(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail;
        Button btnAdmin, btnBan;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnAdmin = itemView.findViewById(R.id.btnToggleAdmin);
            btnBan = itemView.findViewById(R.id.btnToggleBan);
        }
    }
}