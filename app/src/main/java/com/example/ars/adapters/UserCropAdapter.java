package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.IndividualUserCrop;
import java.util.List;

public class UserCropAdapter extends RecyclerView.Adapter<UserCropAdapter.ViewHolder> {

    private List<IndividualUserCrop> crops;
    private OnCropClickListener listener;

    public interface OnCropClickListener {
        void onCropClick(IndividualUserCrop crop);
    }

    public UserCropAdapter(List<IndividualUserCrop> crops, OnCropClickListener listener) {
        this.crops = crops;
        this.listener = listener;
    }

    public void updateList(List<IndividualUserCrop> newList) {
        this.crops = newList;
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
        IndividualUserCrop crop = crops.get(position);

        String displayName = crop.getName();
        if (crop.getVariety() != null && !crop.getVariety().isEmpty()) {
            displayName += " (" + crop.getVariety() + ")";
        }
        holder.tvName.setText(displayName);

        holder.itemView.setOnClickListener(v -> listener.onCropClick(crop));
    }

    @Override
    public int getItemCount() { return crops.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlantName);
        }
    }
}