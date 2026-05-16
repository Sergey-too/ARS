package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.History;
import com.example.ars.models.WeatherData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<History> items;

    public HistoryAdapter(List<History> items) { this.items = items; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        History item = items.get(position);

        // Универсальный парсинг даты: меняем 'T' на пробел для SimpleDateFormat
        String rawDate = item.getDoneAt() != null ? item.getDoneAt().replace("T", " ") : "";
        holder.tvDate.setText(formatDate(rawDate, "dd MMMM"));
        holder.tvTime.setText(formatDate(rawDate, "HH:mm"));

        holder.tvAction.setText(item.getActionName());
        holder.tvCrop.setText(item.getCropName() + " (" + (item.getVariety() != null ? item.getVariety() : "н/д") + ")");
        holder.tvArea.setText("Участок: " + item.getAreaName());

        // Безопасная установка погоды
        if (holder.tvWeather != null) {
            if (item.getWeather() != null) {
                WeatherData w = item.getWeather();
                holder.tvWeather.setText(String.format("🌡️ %s..%s°C | 💧 %s%% | 🌧️ %s мм",
                        w.getTemperatureMin(), w.getTemperatureMax(), w.getHumidityMax(), w.getPrecipitation()));
            } else {
                holder.tvWeather.setText("☁️ Данные о погоде не найдены");
            }
        }

        holder.tvDetails.setText(String.format("Интервалы: Полив %dд | Удобр. %dд | Почва %dд",
                item.getWateringInterval(), item.getFertilizingInterval(), item.getSoilCareInterval()));
    }

    private String formatDate(String raw, String pattern) {
        if (raw == null || raw.isEmpty()) return "--";
        try {
            String cleanDate = raw.contains(".") ? raw.substring(0, raw.indexOf(".")) : raw;
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date date = in.parse(cleanDate);
            return new SimpleDateFormat(pattern, new Locale("ru")).format(date);
        } catch (Exception e) {
            return raw;
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvAction, tvCrop, tvArea, tvWeather, tvDetails;
        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvHistoryDate);
            tvTime = v.findViewById(R.id.tvHistoryTime);
            tvAction = v.findViewById(R.id.tvHistoryAction);
            tvCrop = v.findViewById(R.id.tvHistoryCrop);
            tvArea = v.findViewById(R.id.tvHistoryArea);
            tvWeather = v.findViewById(R.id.tvWeather);
            tvDetails = v.findViewById(R.id.tvHistoryDetails);
        }
    }

    public void updateList(List<History> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }
}
