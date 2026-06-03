package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.History;

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

        String rawDate = item.getDoneAt() != null ? item.getDoneAt().replace("T", " ") : "";
        holder.tvDate.setText(formatDate(rawDate, "dd MMMM"));
        holder.tvTime.setText(formatDate(rawDate, "HH:mm"));

        holder.tvAction.setText(item.getActionName());

        String cropText = item.getCropName() != null ? item.getCropName() : "---";
        String varietyText = item.getVariety() != null && !item.getVariety().isEmpty() ? item.getVariety() : "н/д";
        holder.tvCrop.setText(cropText + " (" + varietyText + ")");

        // Отображаем огород и участок в одной строке (как у вас в XML используется tvArea)
        String gardenText = item.getGardenName() != null && !item.getGardenName().isEmpty() ? item.getGardenName() : "Без огорода";
        String areaText = item.getAreaName() != null ? item.getAreaName() : "---";
        holder.tvArea.setText(gardenText + " | " + areaText);
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

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvAction, tvCrop, tvArea;

        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvHistoryDate);
            tvTime = v.findViewById(R.id.tvHistoryTime);
            tvAction = v.findViewById(R.id.tvHistoryAction);
            tvCrop = v.findViewById(R.id.tvHistoryCrop);
            tvArea = v.findViewById(R.id.tvHistoryArea);
        }
    }

    public void updateList(List<History> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }
}