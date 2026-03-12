package com.personal.smsapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.personal.smsapp.R;
import com.personal.smsapp.data.local.Message;
import com.personal.smsapp.util.PhoneUtils;

public class MessageAdapter extends ListAdapter<Message, MessageAdapter.ViewHolder> {

    private static final int VIEW_TYPE_INCOMING = 0;
    private static final int VIEW_TYPE_OUTGOING = 1;

    public interface OnMessageLongClickListener {
        void onLongClick(Message message);
    }

    private OnMessageLongClickListener longClickListener;

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    public MessageAdapter() {
        super(DIFF);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isIncoming() ? VIEW_TYPE_INCOMING : VIEW_TYPE_OUTGOING;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_TYPE_INCOMING
            ? R.layout.item_message_incoming
            : R.layout.item_message_outgoing;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = getItem(position);
        holder.bind(msg);
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(msg);
            }
            return true;
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBody;
        private final TextView tvTime;
        private final Chip     chipTag;
        private final View     importantIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvBody             = itemView.findViewById(R.id.tv_message_body);
            tvTime             = itemView.findViewById(R.id.tv_message_time);
            chipTag            = itemView.findViewById(R.id.chip_message_tag);
            importantIndicator = itemView.findViewById(R.id.view_important_indicator);
        }

        void bind(Message msg) {
            tvBody.setText(msg.body);
            tvTime.setText(PhoneUtils.formatFullTimestamp(msg.date));

            if (chipTag != null) {
                if (msg.hasTag()) {
                    chipTag.setVisibility(View.VISIBLE);
                    chipTag.setText(msg.apiTag);
                } else {
                    chipTag.setVisibility(View.GONE);
                }
            }

            if (importantIndicator != null) {
                importantIndicator.setVisibility(msg.isImportant ? View.VISIBLE : View.GONE);
            }
        }
    }

    private static final DiffUtil.ItemCallback<Message> DIFF =
        new DiffUtil.ItemCallback<Message>() {
            @Override
            public boolean areItemsTheSame(@NonNull Message a, @NonNull Message b) {
                return a.id == b.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull Message a, @NonNull Message b) {
                return a.body.equals(b.body)
                    && a.read == b.read
                    && a.apiTag.equals(b.apiTag)
                    && a.isImportant == b.isImportant;
            }
        };
}
