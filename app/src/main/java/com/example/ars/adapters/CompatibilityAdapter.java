package com.example.ars.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.CompatibilityDTO;
import java.util.List;

public class CompatibilityAdapter extends RecyclerView.Adapter<CompatibilityAdapter.ViewHolder> {

    private final List<CompatibilityDTO> list;

    public CompatibilityAdapter(List<CompatibilityDTO> list) {
        this.list = list;
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

        int color;
        switch (item.getStatus()) {
            case 4: color = Color.parseColor("#4CAF50"); break;
            case 3: color = Color.parseColor("#FFEB3B"); break;
            case 2: color = Color.parseColor("#F44336"); break;
            default: color = Color.parseColor("#D3D3D3"); break;
        }
        holder.colorView.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        TextView tvCropNames;

        public ViewHolder(View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.compatibilityStatusColor);
        }
    }
}