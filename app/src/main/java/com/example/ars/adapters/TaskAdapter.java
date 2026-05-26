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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<TaskItem> tasks;
    private OnTaskCompleteListener listener;

    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.US);

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

        String formattedDate = formatDate(item.getDueDate());
        String dayOfWeek = formatDayOfWeek(item.getDueDate());

        holder.tvDate.setText(formattedDate);
        holder.tvDayOfWeek.setText(dayOfWeek);

        String actionWithEmoji = getActionWithEmoji(item.getActionTypeId(), item.getActionName());
        holder.tvActionType.setText(actionWithEmoji);

        holder.tvCropName.setText(item.getDisplayName());
        holder.tvAreaName.setText("Участок: " + item.getAreaName());

        if (item.getLastDoneAt() != null && !item.getLastDoneAt().isEmpty()) {
            holder.tvLastDone.setText("Последний раз: " + formatDate(item.getLastDoneAt()));
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

    private String getActionWithEmoji(Integer actionTypeId, String actionName) {
        if (actionTypeId == null) return actionName != null ? actionName : "Уход";
        switch (actionTypeId) {
            case 1: return "Посадка";
            case 2: return "Полив";
            case 3: return "Удобрение";
            case 4: return "Уход за почвой";
            case 5: return "Защита";
            case 6: return "Сбор урожая";
            default: return actionName != null ? actionName : "Уход";
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String formatDayOfWeek(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));
            String day = dayFormat.format(date);
            return day.substring(0, 1).toUpperCase() + day.substring(1);
        } catch (ParseException e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayOfWeek, tvActionType, tvCropName, tvAreaName, tvLastDone, tvStatus;
        MaterialButton btnComplete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvActionType = itemView.findViewById(R.id.tvActionType);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvAreaName = itemView.findViewById(R.id.tvAreaName);
            tvLastDone = itemView.findViewById(R.id.tvLastDone);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }
}