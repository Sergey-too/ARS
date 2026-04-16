package com.example.ars.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.models.PlantingRecommendation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlantingAdapter extends RecyclerView.Adapter<PlantingAdapter.ViewHolder> {

    private List<PlantingRecommendation> recommendations = new ArrayList<>(); // Инициализируй сразу

    public PlantingAdapter(List<PlantingRecommendation> recommendations) {
        if (recommendations != null) {
            this.recommendations = recommendations;
        }
    }

    public void updateData(List<PlantingRecommendation> newRecommendations) {
        Log.d("PLANTING_ADAPTER", "updateData вызван: " +
                (newRecommendations != null ? newRecommendations.size() : "null") + " элементов");

        this.recommendations.clear();

        if (newRecommendations != null) {
            this.recommendations.addAll(newRecommendations);
        }

        Log.d("PLANTING_ADAPTER", "Теперь в адаптере: " + this.recommendations.size() + " элементов");

        notifyDataSetChanged();

        Log.d("PLANTING_ADAPTER", "notifyDataSetChanged вызван, getItemCount=" + getItemCount());
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

        holder.tvDate.setText(formatDate(rec.getDate()));
        holder.tvDayOfWeek.setText(getDayOfWeek(rec.getDate()));

        holder.tvCropName.setText(rec.getCropName());
        holder.tvRegion.setText(rec.getRegionName());

        String weatherInfo = String.format(Locale.getDefault(),
                "🌡️ %.1f..%.1f°C  💧 %.0f%%  💨 %.1f м/с",
                rec.getTempMin(),
                rec.getTempMax(),
                rec.getHumMin(),
                rec.getWindMax());
        holder.tvWeather.setText(weatherInfo);

        holder.tvReason.setText(rec.getReason());

        if (rec.isGoodDay()) {
            holder.tvStatus.setText("БЛАГОПРИЯТНЫЙ ДЕНЬ");
            holder.tvStatus.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvStatus.setText("НЕБЛАГОПРИЯТНЫЙ ДЕНЬ");
            holder.tvStatus.setBackgroundColor(Color.parseColor("#E53935"));
        }
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String getDayOfWeek(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            String[] days = {"Воскресенье", "Понедельник", "Вторник", "Среда",
                    "Четверг", "Пятница", "Суббота"};
            return days[calendar.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (ParseException e) {
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvDayOfWeek;
        TextView tvCropName;
        TextView tvRegion;
        TextView tvWeather;
        TextView tvReason;
        TextView tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvRegion = itemView.findViewById(R.id.tvRegion);
            tvWeather = itemView.findViewById(R.id.tvWeather);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}