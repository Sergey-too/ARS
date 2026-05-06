package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.models.PlantingRecommendation;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class PlantingAdapter extends RecyclerView.Adapter<PlantingAdapter.ViewHolder> {

    private List<PlantingRecommendation> recommendations = new ArrayList<>();

    public void updateData(List<PlantingRecommendation> newRecommendations) {
        this.recommendations.clear();
        if (newRecommendations != null) {
            this.recommendations.addAll(newRecommendations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_planting_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlantingRecommendation rec = recommendations.get(position);

        holder.tvDate.setText(rec.getDate());
        holder.tvDayOfWeek.setText(rec.getDayOfWeek());
        holder.tvCropName.setText(rec.getCropName());
        holder.tvRegion.setText(rec.getAreaName());
        holder.tvWeather.setText(rec.getWeatherText());
        holder.tvReason.setText(rec.getReason());
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayOfWeek, tvCropName, tvRegion, tvWeather, tvReason;
        MaterialButton btnPlant;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvRegion = itemView.findViewById(R.id.tvRegion);
            tvWeather = itemView.findViewById(R.id.tvWeather);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnPlant = itemView.findViewById(R.id.btnPlant);
        }
    }
}