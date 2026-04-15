package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.Area;
import java.util.List;

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.AreaViewHolder> {

    private List<Area> areas;
    private OnAreaClickListener listener;

    public interface OnAreaClickListener {
        void onAreaClick(Area area);
    }

    public AreaAdapter(List<Area> areas, OnAreaClickListener listener) {
        this.areas = areas;
        this.listener = listener;
    }

    public void updateList(List<Area> newAreas) {
        this.areas = newAreas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AreaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new AreaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AreaViewHolder holder, int position) {
        Area area = areas.get(position);
        holder.tvName.setText(area.getName());
        holder.tvRegion.setText(area.getRegion() != null ? area.getRegion().getName() : "Регион не указан");
        holder.itemView.setOnClickListener(v -> listener.onAreaClick(area));
    }

    @Override
    public int getItemCount() { return areas.size(); }

    static class AreaViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRegion;
        public AreaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
            tvRegion = itemView.findViewById(android.R.id.text2);
            // Немного стилизуем программно, если нет своей разметки item_area
            tvName.setTextSize(18);
            tvName.setPadding(0, 8, 0, 4);
        }
    }
}