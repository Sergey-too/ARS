package com.example.ars.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ars.R;
import com.example.ars.models.SupportMessage;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<SupportMessage> messages;
    private int currentUserId;

    public MessageAdapter(List<SupportMessage> messages, int currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupportMessage msg = messages.get(position);
        holder.tvText.setText(msg.getMessageText());
        holder.tvTime.setText(msg.getCreatedAt() != null ? msg.getCreatedAt().substring(11, 16) : "");

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.container.getLayoutParams();

        if (msg.getSenderId() == currentUserId) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            holder.container.setBackgroundResource(android.R.color.holo_green_light);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            holder.container.setBackgroundResource(android.R.color.white);
        }
        holder.container.setLayoutParams(params);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView tvText, tvTime;
        public ViewHolder(View v) {
            super(v);
            container = v.findViewById(R.id.messageContainer);
            tvText = v.findViewById(R.id.tvMessageText);
            tvTime = v.findViewById(R.id.tvMessageTime);
        }
    }
}