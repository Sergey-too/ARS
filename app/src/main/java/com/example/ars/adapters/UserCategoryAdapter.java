package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.models.UserCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserCategoryAdapter extends RecyclerView.Adapter<UserCategoryAdapter.UserCategoryViewHolder> {

    private List<UserCategory> categories;
    private Consumer<UserCategory> onClick;
    private Consumer<UserCategory> onLongClick;

    public UserCategoryAdapter(List<UserCategory> categories, Consumer<UserCategory> onClick, Consumer<UserCategory> onLongClick) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public UserCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new UserCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserCategoryViewHolder holder, int position) {
        UserCategory category = categories.get(position);
        holder.tvName.setText(category.getName());

        holder.itemView.setOnClickListener(v -> onClick.accept(category));
        holder.itemView.setOnLongClickListener(v -> {
            onLongClick.accept(category);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateList(List<UserCategory> newList) {
        this.categories = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class UserCategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public UserCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}