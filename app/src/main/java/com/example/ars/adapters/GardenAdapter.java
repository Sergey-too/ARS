package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.models.Garden;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GardenAdapter extends RecyclerView.Adapter<GardenAdapter.GardenViewHolder> {

    private List<Garden> gardens;
    private Consumer<Garden> onClick;

    public GardenAdapter(List<Garden> gardens, Consumer<Garden> onClick) {
        this.gardens = gardens != null ? gardens : new ArrayList<>();
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public GardenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_garden, parent, false);
        return new GardenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GardenViewHolder holder, int position) {
        Garden garden = gardens.get(position);
        holder.tvName.setText(garden.getName());

        holder.itemView.setOnClickListener(v -> onClick.accept(garden));
    }

    @Override
    public int getItemCount() {
        return gardens.size();
    }

    public void updateList(List<Garden> newList) {
        this.gardens = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class GardenViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public GardenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGardenName);
        }
    }
}