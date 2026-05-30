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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER_OVERDUE = 0;
    private static final int TYPE_HEADER_TODAY = 1;
    private static final int TYPE_HEADER_FUTURE = 2;
    private static final int TYPE_TASK = 3;

    private List<TaskItem> overdueTasks = new ArrayList<>();
    private List<TaskItem> todayTasks = new ArrayList<>();
    private List<TaskItem> futureTasks = new ArrayList<>();

    private OnTaskCompleteListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));
    private SimpleDateFormat lastDoneFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));

    public interface OnTaskCompleteListener {
        void onTaskComplete(TaskItem task);
    }

    public TaskAdapter(OnTaskCompleteListener listener) {
        this.listener = listener;
    }

    public void setGroupedData(List<TaskItem> overdue, List<TaskItem> today, List<TaskItem> future) {
        this.overdueTasks = overdue != null ? overdue : new ArrayList<>();
        this.todayTasks = today != null ? today : new ArrayList<>();
        this.futureTasks = future != null ? future : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateTasks(List<TaskItem> tasks) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date todayDate = new Date();

        overdueTasks.clear();
        todayTasks.clear();
        futureTasks.clear();

        for (TaskItem task : tasks) {
            if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
                futureTasks.add(task);
                continue;
            }

            try {
                Date taskDate = sdf.parse(task.getDueDate());
                if (taskDate != null) {
                    if (taskDate.before(todayDate)) {
                        overdueTasks.add(task);
                    } else if (dateEquals(taskDate, todayDate)) {
                        todayTasks.add(task);
                    } else {
                        futureTasks.add(task);
                    }
                } else {
                    futureTasks.add(task);
                }
            } catch (Exception e) {
                futureTasks.add(task);
            }
        }

        notifyDataSetChanged();
    }

    private boolean dateEquals(Date d1, Date d2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(d1).equals(sdf.format(d2));
    }

    @Override
    public int getItemViewType(int position) {
        int overdueSize = overdueTasks.size();
        int todaySize = todayTasks.size();

        if (position == 0 && overdueSize > 0) {
            return TYPE_HEADER_OVERDUE;
        }
        if (position > 0 && position <= overdueSize) {
            return TYPE_TASK;
        }

        int offset = overdueSize > 0 ? overdueSize + 1 : 0;
        if (position == offset && todaySize > 0) {
            return TYPE_HEADER_TODAY;
        }
        if (position > offset && position <= offset + todaySize) {
            return TYPE_TASK;
        }

        offset = (overdueSize > 0 ? overdueSize + 1 : 0) + (todaySize > 0 ? todaySize + 1 : 0);
        if (position == offset && futureTasks.size() > 0) {
            return TYPE_HEADER_FUTURE;
        }
        return TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER_OVERDUE || viewType == TYPE_HEADER_TODAY || viewType == TYPE_HEADER_FUTURE) {
            View view = inflater.inflate(R.layout.item_task_header, parent, false);
            return new HeaderViewHolder(view, viewType);
        } else {
            View view = inflater.inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int overdueSize = overdueTasks.size();
        int todaySize = todayTasks.size();

        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            switch (headerHolder.headerType) {
                case TYPE_HEADER_OVERDUE:
                    headerHolder.tvHeader.setText("Просроченные задачи");
                    headerHolder.tvCount.setText("(" + overdueTasks.size() + ")");
                    break;
                case TYPE_HEADER_TODAY:
                    headerHolder.tvHeader.setText("На сегодня");
                    headerHolder.tvCount.setText("(" + todayTasks.size() + ")");
                    break;
                case TYPE_HEADER_FUTURE:
                    headerHolder.tvHeader.setText("Предстоящие задачи");
                    headerHolder.tvCount.setText("(" + futureTasks.size() + ")");
                    break;
            }
        } else if (holder instanceof TaskViewHolder) {
            TaskItem task = getTaskAtPosition(position);
            if (task != null) {
                TaskViewHolder taskHolder = (TaskViewHolder) holder;

                String formattedDate = "";
                String dayOfWeek = "";
                if (task.getDueDate() != null) {
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(task.getDueDate());
                        if (date != null) {
                            formattedDate = dateFormat.format(date);
                            dayOfWeek = dayFormat.format(date);
                        }
                    } catch (Exception e) {
                        formattedDate = task.getDueDate();
                        dayOfWeek = "";
                    }
                }

                taskHolder.tvDate.setText(formattedDate);
                taskHolder.tvDayOfWeek.setText(dayOfWeek);
                taskHolder.tvActionType.setText(getActionIconAndName(task.getActionTypeId()));
                taskHolder.tvCropName.setText(task.getCropName());
                if (task.getVariety() != null && !task.getVariety().isEmpty()) {
                    taskHolder.tvCropName.setText(task.getCropName() + " (" + task.getVariety() + ")");
                }
                taskHolder.tvAreaName.setText("Участок: " + (task.getAreaName() != null ? task.getAreaName() : "---"));

                if (isOverdue(task)) {
                    taskHolder.tvStatus.setVisibility(View.VISIBLE);
                    taskHolder.tvStatus.setText("Просрочено");
                    taskHolder.tvStatus.setTextColor(0xFFF44336);
                } else {
                    taskHolder.tvStatus.setVisibility(View.GONE);
                }

                if (task.getLastDoneAt() != null && !task.getLastDoneAt().isEmpty()) {
                    try {
                        Date lastDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(task.getLastDoneAt());
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
    }

    private boolean isOverdue(TaskItem task) {
        if (task.getDueDate() == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date taskDate = sdf.parse(task.getDueDate());
            Date today = new Date();
            return taskDate != null && taskDate.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    private TaskItem getTaskAtPosition(int position) {
        int overdueSize = overdueTasks.size();
        int todaySize = todayTasks.size();

        if (position == 0 && overdueSize > 0) return null;
        if (position > 0 && position <= overdueSize) {
            return overdueTasks.get(position - 1);
        }

        int offset = overdueSize > 0 ? overdueSize + 1 : 0;
        if (position == offset && todaySize > 0) return null;
        if (position > offset && position <= offset + todaySize) {
            return todayTasks.get(position - offset - 1);
        }

        offset = (overdueSize > 0 ? overdueSize + 1 : 0) + (todaySize > 0 ? todaySize + 1 : 0);
        if (position > offset && position <= offset + futureTasks.size()) {
            return futureTasks.get(position - offset - 1);
        }

        return null;
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
        int count = 0;
        if (!overdueTasks.isEmpty()) count += overdueTasks.size() + 1;
        if (!todayTasks.isEmpty()) count += todayTasks.size() + 1;
        if (!futureTasks.isEmpty()) count += futureTasks.size() + 1;
        return count;
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

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader, tvCount;
        LinearLayout container;
        int headerType;

        HeaderViewHolder(View itemView, int headerType) {
            super(itemView);
            this.headerType = headerType;
            tvHeader = itemView.findViewById(R.id.tvHeader);
            tvCount = itemView.findViewById(R.id.tvCount);
            container = itemView.findViewById(R.id.container);
        }
    }
}