package com.example.ars.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.TaskItem;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TASK = 0;

    private List<TaskItem> tasks = new ArrayList<>();

    private OnTaskCompleteListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));
    private SimpleDateFormat lastDoneFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnTaskCompleteListener {
        void onTaskComplete(TaskItem task);
    }

    public TaskAdapter(OnTaskCompleteListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setGroupedData(List<TaskItem> overdue, List<TaskItem> today, List<TaskItem> future) {
        tasks.clear();
        if (overdue != null) tasks.addAll(overdue);
        if (today != null) tasks.addAll(today);
        if (future != null) tasks.addAll(future);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TaskItem task = tasks.get(position);
        if (task != null && holder instanceof TaskViewHolder) {
            TaskViewHolder taskHolder = (TaskViewHolder) holder;

            String formattedDate = "";
            String dayOfWeek = "";
            if (task.getDueDate() != null) {
                try {
                    Date date = sdf.parse(task.getDueDate());
                    if (date != null) {
                        formattedDate = dateFormat.format(date);
                        dayOfWeek = dayFormat.format(date);
                        if (dayOfWeek.length() > 0) {
                            dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1);
                        }
                    }
                } catch (Exception e) {
                    formattedDate = task.getDueDate();
                    dayOfWeek = "";
                }
            }

            taskHolder.tvDate.setText(formattedDate);
            taskHolder.tvDayOfWeek.setText(dayOfWeek);
            taskHolder.tvActionType.setText(getActionIconAndName(task.getActionTypeId()));

            String cropText = task.getCropName() != null ? task.getCropName() : "---";
            if (task.getVariety() != null && !task.getVariety().isEmpty() && !task.getVariety().equals("Обычный")) {
                cropText = cropText + " (" + task.getVariety() + ")";
            }
            taskHolder.tvCropName.setText(cropText);

            String locationText = "";
            if (task.getGardenName() != null && !task.getGardenName().isEmpty()) {
                locationText = task.getGardenName();
                if (task.getAreaName() != null && !task.getAreaName().isEmpty()) {
                    locationText = locationText + " - " + task.getAreaName();
                }
            } else if (task.getAreaName() != null && !task.getAreaName().isEmpty()) {
                locationText = task.getAreaName();
            } else {
                locationText = "---";
            }
            taskHolder.tvAreaName.setText(locationText);

            if (isOverdue(task)) {
                taskHolder.tvStatus.setVisibility(View.VISIBLE);
                taskHolder.tvStatus.setText("Просрочено");
                taskHolder.tvStatus.setTextColor(0xFFF44336);
            } else {
                taskHolder.tvStatus.setVisibility(View.GONE);
            }

            if (task.getLastDoneAt() != null && !task.getLastDoneAt().isEmpty()) {
                try {
                    Date lastDate = sdf.parse(task.getLastDoneAt());
                    if (lastDate != null) {
                        taskHolder.tvLastDone.setText("Последний раз: " + lastDoneFormat.format(lastDate));
                    } else {
                        taskHolder.tvLastDone.setText("Последний раз: не выполнялось");
                    }
                } catch (Exception e) {
                    taskHolder.tvLastDone.setText("Последний раз: не выполнялось");
                }
            } else {
                taskHolder.tvLastDone.setText("Последний раз: не выполнялось");
            }

            taskHolder.btnComplete.setOnClickListener(v -> listener.onTaskComplete(task));
        }
    }

    private boolean isOverdue(TaskItem task) {
        if (task.getDueDate() == null) return false;
        try {
            Date taskDate = sdf.parse(task.getDueDate());

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();

            return taskDate != null && taskDate.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    private String getActionIconAndName(Integer actionTypeId) {
        if (actionTypeId == null) return "Уход";
        switch (actionTypeId) {
            case 1: return "Посадка";
            case 2: return "Полив";
            case 3: return "Удобрение";
            case 4: return "Уход за почвой";
            case 5: return "Защита";
            case 6: return "Сбор урожая";
            default: return "Уход";
        }
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayOfWeek, tvActionType, tvStatus, tvCropName, tvAreaName, tvLastDone;
        MaterialButton btnComplete;

        TaskViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvActionType = itemView.findViewById(R.id.tvActionType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvAreaName = itemView.findViewById(R.id.tvAreaName);
            tvLastDone = itemView.findViewById(R.id.tvLastDone);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }
}