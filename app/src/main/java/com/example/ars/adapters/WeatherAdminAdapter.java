package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.models.WeatherData;

import java.util.ArrayList;
import java.util.List;

public class WeatherAdminAdapter extends RecyclerView.Adapter<WeatherAdminAdapter.ViewHolder> {

    private List<WeatherData> weatherList = new ArrayList<>();
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDelete(WeatherData weather, int position);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public void setData(List<WeatherData> data) {
        weatherList = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherData w = weatherList.get(position);

        // Форматируем дату из 2025-05-06 в 06.05.2025
        String formattedDate = formatDate(w.getDate());
        holder.tvDate.setText(formattedDate);

        holder.tvTemp.setText(w.getTempRange());

        holder.tvHumidity.setText(w.getHumidityRange());

        holder.tvWindPrecip.setText(w.getWindPrecipText());

        holder.tvPressure.setText(w.getPressureText());

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(w, position);
            }
        });
    }

    private String formatDate(String date) {
        if (date == null || date.isEmpty()) return "---";
        if (date.contains("-")) {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                return parts[2] + "." + parts[1] + "." + parts[0];
            }
        }
        return date;
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTemp, tvHumidity, tvWindPrecip, tvPressure;
        ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvHumidity = itemView.findViewById(R.id.tvHumidity);
            tvWindPrecip = itemView.findViewById(R.id.tvWindPrecip);
            tvPressure = itemView.findViewById(R.id.tvPressure);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}