package com.example.ars;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

        // Очищаем старый список
        this.recommendations.clear();

        // Добавляем новые данные
        if (newRecommendations != null) {
            this.recommendations.addAll(newRecommendations);
        }

        Log.d("PLANTING_ADAPTER", "Теперь в адаптере: " + this.recommendations.size() + " элементов");

        // Уведомляем об изменениях
        notifyDataSetChanged();

        // Проверяем, что уведомление сработало
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

        // ЛОГИРОВАНИЕ
        Log.d("PLANTING_ADAPTER", "Позиция: " + position);
        Log.d("PLANTING_ADAPTER", "Дата: " + rec.getDate());
        Log.d("PLANTING_ADAPTER", "Растение: " + rec.getCropName());
        Log.d("PLANTING_ADAPTER", "Температура: " + rec.getWeatherTemperature());
        Log.d("PLANTING_ADAPTER", "Влажность: " + rec.getWeatherHumidity());
        Log.d("PLANTING_ADAPTER", "Причина: " + rec.getReason());

        // Форматируем дату
        String formattedDate = formatDate(rec.getDate());
        String dayOfWeek = getDayOfWeek(rec.getDate());

        holder.tvDate.setText(formattedDate);
        holder.tvDayOfWeek.setText(dayOfWeek);
        holder.tvCropName.setText(rec.getCropName());
        holder.tvRegion.setText(rec.getRegionName());

        // Устанавливаем погоду и рекомендацию
        if (rec.getWeatherTemperature() != null && rec.getWeatherHumidity() != null && rec.getWeatherWind() != null) {
            String weatherText = "🌡️ " + rec.getWeatherTemperature() +
                    "  💧 " + rec.getWeatherHumidity() +
                    "  💨 " + rec.getWeatherWind();
            holder.tvWeather.setText(weatherText);
        } else {
            holder.tvWeather.setText("Нет данных о погоде");
        }

        holder.tvReason.setText(rec.getReason() != null ? rec.getReason() : "Благоприятные условия");

        // Устанавливаем статус
        String status = rec.isGoodDay() ? "БЛАГОПРИЯТНЫЙ ДЕНЬ" : "НЕ РЕКОМЕНДУЕТСЯ";
        int statusColor = rec.isGoodDay() ?
                android.graphics.Color.parseColor("#4CAF50") : // Зеленый
                android.graphics.Color.parseColor("#F44336");  // Красный

        holder.tvStatus.setText(status);
        holder.tvStatus.setBackgroundColor(statusColor);
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