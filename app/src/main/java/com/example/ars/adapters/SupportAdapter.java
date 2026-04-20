package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ars.R;
import com.example.ars.models.SupportRequest;

import java.util.List;

public class SupportAdapter extends RecyclerView.Adapter<SupportAdapter.ViewHolder> {
    private List<SupportRequest> list;
    private OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onEdit(SupportRequest request);
        void onDelete(Integer id);
    }

    public SupportAdapter(List<SupportRequest> list, OnRequestClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support, parent, false);
        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupportRequest req = list.get(position);
        holder.tvSubject.setText(req.getSubject());

        if (req.getCreatedAt() != null) {
            holder.tvDate.setText(req.getCreatedAt());
        } else {
            holder.tvDate.setText("Недавно");
        }

        String statusText;
        int statusColor;
        switch (req.getStatusId()) {
            case 1: statusText = "Новый"; statusColor = 0xFF2196F3; break; // Синий
            case 3: statusText = "В работе"; statusColor = 0xFFFF9800; break; // Оранжевый
            case 4: statusText = "Закрыт"; statusColor = 0xFF4CAF50; break; // Зеленый
            default: statusText = "Отправлено"; statusColor = 0xFF757575; // Серый
        }
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setTextColor(statusColor);

        holder.itemView.setOnClickListener(v -> listener.onEdit(req));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onDelete(req.getId());
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvStatus, tvDate;
        public ViewHolder(View v) {
            super(v);
            tvSubject = v.findViewById(R.id.tvSubject);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }
}