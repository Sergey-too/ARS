package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.models.Region;

import java.util.List;

public class RegionAdapter extends RecyclerView.Adapter<RegionAdapter.RegionViewHolder> {

    private List<Region> regions;
    private OnRegionClickListener listener;

    public interface OnRegionClickListener {
        void onRegionClick(Region region);
    }

    public RegionAdapter(List<Region> regions, OnRegionClickListener listener) {
        this.regions = regions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RegionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_region, parent, false);
        return new RegionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RegionViewHolder holder, int position) {
        Region region = regions.get(position);
        holder.tvName.setText(region.getName());

        holder.itemView.setOnClickListener(v -> listener.onRegionClick(region));
        holder.btnEdit.setOnClickListener(v -> listener.onRegionClick(region));
    }

    @Override
    public int getItemCount() {
        return regions.size();
    }

    public static class RegionViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView btnEdit;

        public RegionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRegionName);
        }
    }
}