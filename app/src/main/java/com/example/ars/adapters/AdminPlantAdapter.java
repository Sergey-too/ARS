package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.Crop;
import java.util.List;

public class AdminPlantAdapter extends RecyclerView.Adapter<AdminPlantAdapter.ViewHolder> {
    private List<Crop> plants;
    private OnPlantClickListener listener;

    public interface OnPlantClickListener {
        void onPlantClick(Crop plant);
    }

    public AdminPlantAdapter(List<Crop> plants, OnPlantClickListener listener) {
        this.plants = plants;
        this.listener = listener;
    }

    public void updateData(List<Crop> newPlants) {
        this.plants = newPlants;
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
        Crop plant = plants.get(position);
        holder.tvName.setText(plant.getName() != null ? plant.getName() : "Без названия");
        holder.itemView.setOnClickListener(v -> listener.onPlantClick(plant));
    }

    @Override
    public int getItemCount() {
        return plants != null ? plants.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlantName);
        }
    }
}