package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.TaskItem;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<TaskItem> tasks;
    private OnTaskCompleteListener listener;

    public interface OnTaskCompleteListener {
        void onTaskComplete(TaskItem task);
    }

    public TaskAdapter(List<TaskItem> tasks, OnTaskCompleteListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void updateData(List<TaskItem> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskItem item = tasks.get(position);

        holder.tvDate.setText(item.getDueDate() != null ? item.getDueDate() : "");
        holder.tvActionType.setText(item.getActionName());
        holder.tvCropName.setText(item.getDisplayName());
        holder.tvAreaName.setText("Участок: " + item.getAreaName());

        if (item.getLastDoneAt() != null) {
            holder.tvLastDone.setText("Последний раз: " + item.getLastDoneAt());
        } else {
            holder.tvLastDone.setText("Ни разу не выполнялось");
        }

        if (item.getIsOverdue() != null && item.getIsOverdue()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("Просрочено");
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        holder.btnComplete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskComplete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvActionType, tvCropName, tvAreaName, tvLastDone, tvStatus;
        MaterialButton btnComplete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvActionType = itemView.findViewById(R.id.tvActionType);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvAreaName = itemView.findViewById(R.id.tvAreaName);
            tvLastDone = itemView.findViewById(R.id.tvLastDone);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }
}