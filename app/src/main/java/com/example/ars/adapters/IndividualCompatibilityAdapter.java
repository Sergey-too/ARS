package com.example.ars.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.IndividualCompatibilityDTO;
import java.util.List;

public class IndividualCompatibilityAdapter extends RecyclerView.Adapter<IndividualCompatibilityAdapter.ViewHolder> {

    private List<IndividualCompatibilityDTO> data;
    private List<String> cropNames;
    private OnCellClickListener listener;

    public interface OnCellClickListener {
        void onCellClick(int crop1Id, int crop2Id, int currentStatus, int position);
    }

    public IndividualCompatibilityAdapter(List<IndividualCompatibilityDTO> data, List<String> cropNames, OnCellClickListener listener) {
        this.data = data;
        this.cropNames = cropNames;
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
        IndividualCompatibilityDTO item = data.get(position);

        int status = item.getStatus() != null ? item.getStatus() : 1;  // getStatus()
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
                listener.onCellClick(item.getCrop1Id(), item.getCrop2Id(), status, position);
            }
        });
    }

    public void updateCell(int position, int newStatus) {
        data.get(position).setStatus(newStatus);
        notifyItemChanged(position);
    }

    public String getCropNameByPosition(int index) {
        if (index >= 0 && index < cropNames.size()) {
            return cropNames.get(index);
        }
        return "";
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        ViewHolder(View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.compatibilityStatusColor);
        }
    }
}