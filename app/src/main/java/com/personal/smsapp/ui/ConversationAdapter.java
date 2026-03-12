package com.personal.smsapp.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import com.personal.smsapp.data.local.Conversation;
import com.personal.smsapp.util.PhoneUtils;

public class ConversationAdapter
    extends ListAdapter<Conversation, ConversationAdapter.ViewHolder> {

    /** Distinct Material-palette colours for contact avatars. */
    private static final int[] AVATAR_COLORS = {
        0xFF5C6BC0, // Indigo 400
        0xFF42A5F5, // Blue 400
        0xFF26A69A, // Teal 400
        0xFFAB47BC, // Purple 400
        0xFFEF5350, // Red 400
        0xFFFF7043, // Deep Orange 400
        0xFF66BB6A, // Green 400
        0xFF8D6E63, // Brown 400
        0xFF78909C, // Blue Grey 400
        0xFFEC407A, // Pink 400
    };

    public interface OnItemClick     { void onClick(Conversation c); }
    public interface OnItemLongClick { boolean onLongClick(Conversation c); }

    private final OnItemClick     clickListener;
    private final OnItemLongClick longClickListener;

    public ConversationAdapter(OnItemClick click, OnItemLongClick longClick) {
        super(DIFF);
        this.clickListener     = click;
        this.longClickListener = longClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvSnippet;
        private final TextView tvTime;
        private final TextView tvUnread;
        private final Chip     chipTag;
        private final View     importantDot;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar     = itemView.findViewById(R.id.view_avatar);
            tvName       = itemView.findViewById(R.id.tv_name);
            tvSnippet    = itemView.findViewById(R.id.tv_snippet);
            tvTime       = itemView.findViewById(R.id.tv_time);
            tvUnread     = itemView.findViewById(R.id.tv_unread_count);
            chipTag      = itemView.findViewById(R.id.chip_tag);
            importantDot = itemView.findViewById(R.id.view_important_dot);
        }

        void bind(Conversation c) {
            String displayName = (c.displayName != null && !c.displayName.isEmpty())
                ? c.displayName
                : (c.address != null ? c.address : "?");

            tvName.setText(displayName);
            tvSnippet.setText(c.snippet);
            tvTime.setText(PhoneUtils.formatTimestamp(c.date));

            // Avatar: first-letter initial with per-contact hashed colour
            tvAvatar.setText(String.valueOf(Character.toUpperCase(displayName.charAt(0))));
            int colorIndex = Math.abs(displayName.hashCode()) % AVATAR_COLORS.length;
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(AVATAR_COLORS[colorIndex]);
            tvAvatar.setBackground(avatarBg);
            // Bold for unread
            int style = c.unreadCount > 0 ? Typeface.BOLD : Typeface.NORMAL;
            tvName.setTypeface(null, style);
            tvSnippet.setTypeface(null, style);

            // Unread badge
            if (c.unreadCount > 0) {
                tvUnread.setVisibility(View.VISIBLE);
                tvUnread.setText(String.valueOf(c.unreadCount));
            } else {
                tvUnread.setVisibility(View.GONE);
            }

            // API tag chip
            if (c.lastTag != null && !c.lastTag.isEmpty()) {
                chipTag.setVisibility(View.VISIBLE);
                chipTag.setText(c.lastTag);
            } else {
                chipTag.setVisibility(View.GONE);
            }

            // Important dot
            importantDot.setVisibility(c.hasImportant ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> clickListener.onClick(c));
            itemView.setOnLongClickListener(v -> longClickListener.onLongClick(c));
        }
    }

    private static final DiffUtil.ItemCallback<Conversation> DIFF =
        new DiffUtil.ItemCallback<Conversation>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation a, @NonNull Conversation b) {
                return a.threadId == b.threadId;
            }
            @Override
            public boolean areContentsTheSame(@NonNull Conversation a, @NonNull Conversation b) {
                return a.snippet.equals(b.snippet)
                    && a.unreadCount == b.unreadCount
                    && a.date == b.date
                    && a.lastTag.equals(b.lastTag)
                    && a.hasImportant == b.hasImportant;
            }
        };
}
