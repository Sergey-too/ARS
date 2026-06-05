package com.example.ars.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.CompatibilityDTO;
import java.util.List;

public class SinglePlantCompatibilityAdapter extends RecyclerView.Adapter<SinglePlantCompatibilityAdapter.ViewHolder> {

    private List<CompatibilityDTO> list;
    private OnCellClickListener listener;

    public interface OnCellClickListener {
        void onCellClick(CompatibilityDTO item, int position);
    }

    public SinglePlantCompatibilityAdapter(List<CompatibilityDTO> list, OnCellClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compatibility_cell, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CompatibilityDTO item = list.get(position);
        int status = item.getStatus() != null ? item.getStatus() : 1;

        int color;
        switch (status) {
            case 4: color = Color.parseColor("#4CAF50"); break;
            case 3: color = Color.parseColor("#FFEB3B"); break;
            case 2: color = Color.parseColor("#F44336"); break;
            default: color = Color.parseColor("#D3D3D3"); break;
        }

        holder.colorView.setBackgroundColor(color);

        holder.colorView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCellClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateCell(int position, int newStatus) {
        list.get(position).setStatus(newStatus);
        notifyItemChanged(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorView;

        ViewHolder(View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.compatibilityStatusColor);
        }
    }
}