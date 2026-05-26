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
    private OnPlantClickListener onPlantClickListener;

    public interface OnPlantClickListener {
        void onPlantClick(PlantingRecommendation item, int position);
    }

    public void setOnPlantClickListener(OnPlantClickListener listener) {
        this.onPlantClickListener = listener;
    }

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

        String displayName = rec.getCropName();
        if (rec.getVariety() != null && !rec.getVariety().isEmpty() && !rec.getVariety().equals("Обычный")) {
            displayName = rec.getCropName() + " (" + rec.getVariety() + ")";
        }
        holder.tvCropName.setText(displayName);

        holder.tvRegion.setText(rec.getAreaName());

        if (rec.getTempCurrent() != null) {
            holder.tvTempCurrent.setText(rec.getTempCurrent());
        }
        if (rec.getTempRequired() != null) {
            holder.tvTempRequired.setText(rec.getTempRequired());
        }

        if (rec.getHumidityCurrent() != null) {
            holder.tvHumidityCurrent.setText(rec.getHumidityCurrent());
        }
        if (rec.getHumidityRequired() != null) {
            holder.tvHumidityRequired.setText(rec.getHumidityRequired());
        }

        if (rec.getPrecipCurrent() != null) {
            holder.tvPrecipCurrent.setText(rec.getPrecipCurrent());
        }
        if (rec.getPrecipRequired() != null) {
            holder.tvPrecipRequired.setText(rec.getPrecipRequired());
        }

        if (rec.getWindCurrent() != null) {
            holder.tvWindCurrent.setText(rec.getWindCurrent());
        }
        if (rec.getWindRequired() != null) {
            holder.tvWindRequired.setText(rec.getWindRequired());
        }

        holder.btnPlant.setOnClickListener(v -> {
            if (onPlantClickListener != null) {
                onPlantClickListener.onPlantClick(rec, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayOfWeek, tvCropName, tvRegion;
        TextView tvTempCurrent, tvTempRequired;
        TextView tvHumidityCurrent, tvHumidityRequired;
        TextView tvPrecipCurrent, tvPrecipRequired;
        TextView tvWindCurrent, tvWindRequired;
        MaterialButton btnPlant;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvRegion = itemView.findViewById(R.id.tvRegion);

            tvTempCurrent = itemView.findViewById(R.id.tvTempCurrent);
            tvTempRequired = itemView.findViewById(R.id.tvTempRequired);
            tvHumidityCurrent = itemView.findViewById(R.id.tvHumidityCurrent);
            tvHumidityRequired = itemView.findViewById(R.id.tvHumidityRequired);
            tvPrecipCurrent = itemView.findViewById(R.id.tvPrecipCurrent);
            tvPrecipRequired = itemView.findViewById(R.id.tvPrecipRequired);
            tvWindCurrent = itemView.findViewById(R.id.tvWindCurrent);
            tvWindRequired = itemView.findViewById(R.id.tvWindRequired);

            btnPlant = itemView.findViewById(R.id.btnPlant);
        }
    }
}