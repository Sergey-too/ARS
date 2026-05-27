package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.WeatherData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {

    private List<WeatherData> weatherList = new ArrayList<>();
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));

    public void setData(List<WeatherData> data) {
        this.weatherList = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherData weather = weatherList.get(position);

        String formattedDate = formatDate(weather.getDate());
        String dayOfWeek = formatDayOfWeek(weather.getDate());

        holder.tvDate.setText(formattedDate);
        holder.tvDayOfWeek.setText(dayOfWeek);

        double tempMin = parseDouble(weather.getTemperatureMin());
        double tempMax = parseDouble(weather.getTemperatureMax());
        holder.tvTemp.setText(String.format(Locale.getDefault(), "%.1f-%.1f°C", tempMin, tempMax));

        double windMin = parseDouble(weather.getWindMin());
        double windMax = parseDouble(weather.getWindMax());
        holder.tvWind.setText(String.format(Locale.getDefault(), "%.1f-%.1f м/с", windMin, windMax));

        String pressure = weather.getPressure() != null ? weather.getPressure() : "--";
        holder.tvPressure.setText(pressure + " гПа");

        double humMin = parseDouble(weather.getHumidityMin());
        double humMax = parseDouble(weather.getHumidityMax());
        holder.tvHumidity.setText(String.format(Locale.getDefault(), "%.0f-%.0f%%", humMin, humMax));

        double precipitation = parseDouble(weather.getPrecipitation());
        holder.tvPrecipitation.setText(String.format(Locale.getDefault(), "%.1f мм", precipitation));
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "--";
        try {
            Date date = inputFormat.parse(rawDate);
            return date != null ? dateFormat.format(date) : rawDate;
        } catch (Exception e) {
            return rawDate;
        }
    }

    private String formatDayOfWeek(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            Date date = inputFormat.parse(rawDate);
            return date != null ? dayFormat.format(date) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayOfWeek, tvTemp, tvWind, tvPressure, tvHumidity, tvPrecipitation;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvWind = itemView.findViewById(R.id.tvWind);
            tvPressure = itemView.findViewById(R.id.tvPressure);
            tvHumidity = itemView.findViewById(R.id.tvHumidity);
            tvPrecipitation = itemView.findViewById(R.id.tvPrecipitation);
        }
    }
}