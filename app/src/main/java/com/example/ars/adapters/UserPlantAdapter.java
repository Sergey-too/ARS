package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.UserCrop;
import java.util.List;

public class UserPlantAdapter extends RecyclerView.Adapter<UserPlantAdapter.ViewHolder> {
    private List<UserCrop> userCrops;
    private OnUserPlantClickListener listener;

    public interface OnUserPlantClickListener {
        void onPlantClick(UserCrop userCrop);
    }

    public UserPlantAdapter(List<UserCrop> userCrops, OnUserPlantClickListener listener) {
        this.userCrops = userCrops;
        this.listener = listener;
    }

    public void updateData(List<UserCrop> newCrops) {
        this.userCrops = newCrops;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserCrop userCrop = userCrops.get(position);
        if (userCrop.getCrop() != null) {
            holder.tvName.setText(userCrop.getCrop().getName());
        } else {
            holder.tvName.setText("Неизвестное растение");
        }
        holder.itemView.setOnClickListener(v -> listener.onPlantClick(userCrop));
    }

    @Override
    public int getItemCount() {
        return userCrops != null ? userCrops.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlantName);
        }
    }
}