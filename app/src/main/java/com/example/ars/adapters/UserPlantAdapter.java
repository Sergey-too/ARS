package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.IndividualUserCrop;
import com.example.ars.models.UserCrop;

import java.util.List;

public class UserPlantAdapter extends RecyclerView.Adapter<UserPlantAdapter.ViewHolder> {

    private List<PlantItem> plants;
    private OnUserPlantClickListener listener;

    public static class PlantItem {
        private UserCrop userCrop;

        public PlantItem(UserCrop crop) {
            this.userCrop = crop;
        }

        public Integer getId() {
            return userCrop.getId();
        }

        public Integer getIndividualCropId() {
            if (userCrop.getIndividualCrop() != null) {
                return userCrop.getIndividualCrop().getId();
            }
            return null;
        }

        public Integer getSystemCropId() {
            if (userCrop.getCrop() != null) {
                return userCrop.getCrop().getId();
            }
            return userCrop.getCropId();
        }

        public boolean isIndividual() {
            return userCrop.getIndividualCrop() != null;
        }

        public String getDisplayName() {
            if (userCrop.getIndividualCrop() != null) {
                String name = userCrop.getIndividualCrop().getName();
                String variety = userCrop.getIndividualCrop().getVariety();
                if (variety != null && !variety.isEmpty()) {
                    return name + " (" + variety + ")";
                }
                return name;
            } else if (userCrop.getCrop() != null) {
                String name = userCrop.getCrop().getName();
                String variety = userCrop.getCrop().getVariety();
                if (variety != null && !variety.isEmpty()) {
                    return name + " (" + variety + ")";
                }
                return name;
            }
            return "Без названия";
        }
    }

    public interface OnUserPlantClickListener {
        void onPlantClick(PlantItem plant);
    }

    public UserPlantAdapter(List<PlantItem> plants, OnUserPlantClickListener listener) {
        this.plants = plants;
        this.listener = listener;
    }

    public void updateData(List<PlantItem> newPlants) {
        this.plants = newPlants;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlantItem item = plants.get(position);

        String displayName = item.getDisplayName();
        holder.tvName.setText(displayName);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlantClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plants != null ? plants.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlantName);
        }
    }
}