package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
        holder.tvLogin.setText(user.getLogin());
        holder.tvEmail.setText(user.getEmail());
        holder.tvDate.setText("Регистрация: " + user.getRegistrationDate());

        // Настройка кнопок
        holder.btnAdmin.setText(user.getIsAdmin() ? "Убрать админа" : "Сделать админом");
        holder.btnBan.setText(user.getInBan() ? "Разбанить" : "Забанить");

        holder.btnAdmin.setOnClickListener(v -> listener.onAdminToggle(user));
        holder.btnBan.setOnClickListener(v -> listener.onBanToggle(user));
    }

    @Override
    public int getItemCount() { return users.size(); }

    public void updateData(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogin, tvEmail, tvDate;
        Button btnAdmin, btnBan;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogin = itemView.findViewById(R.id.tvUserLogin);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvDate = itemView.findViewById(R.id.tvRegDate);
            btnAdmin = itemView.findViewById(R.id.btnToggleAdmin);
            btnBan = itemView.findViewById(R.id.btnToggleBan);
        }
    }
}