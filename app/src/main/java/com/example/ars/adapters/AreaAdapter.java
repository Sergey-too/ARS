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

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.ViewHolder> {

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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Area area = areas.get(position); // исправлено: используем список areas

        holder.tvAreaName.setText(area.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAreaClick(area);
            }
        });
    }

    @Override
    public int getItemCount() {
        return areas != null ? areas.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAreaName;
        ViewHolder(View itemView) {
            super(itemView);
            tvAreaName = itemView.findViewById(R.id.tvPlantName);
        }
    }
}