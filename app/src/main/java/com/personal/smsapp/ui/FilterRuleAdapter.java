package com.personal.smsapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.personal.smsapp.data.local.LocalFilter;
import com.personal.smsapp.databinding.ItemFilterRuleBinding;

public class FilterRuleAdapter extends ListAdapter<LocalFilter, FilterRuleAdapter.ViewHolder> {

    interface Listener {
        void onEdit(LocalFilter filter);
        void onDelete(LocalFilter filter);
        void onToggleEnabled(LocalFilter filter, boolean enabled);
    }

    private final Listener listener;

    FilterRuleAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFilterRuleBinding b = ItemFilterRuleBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFilterRuleBinding b;

        ViewHolder(ItemFilterRuleBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(LocalFilter f) {
            b.tvName.setText(f.name.isEmpty() ? "(unnamed)" : f.name);

            String signalLabel = f.isRegex ? "regex: " + f.signal : f.signal;
            b.tvSignal.setText(signalLabel);

            b.chipTag.setText(f.tag.isEmpty() ? "no tag" : f.tag);
            b.chipTag.setVisibility(View.VISIBLE);

            b.chipServer.setText(f.sendToServer ? "→ server" : "local only");

            // Avoid triggering listener during bind
            b.switchEnabled.setOnCheckedChangeListener(null);
            b.switchEnabled.setChecked(f.enabled);
            b.switchEnabled.setOnCheckedChangeListener((btn, checked) ->
                listener.onToggleEnabled(f, checked));

            b.btnEdit.setOnClickListener(v -> listener.onEdit(f));
            b.btnDelete.setOnClickListener(v -> listener.onDelete(f));
        }
    }

    private static final DiffUtil.ItemCallback<LocalFilter> DIFF =
        new DiffUtil.ItemCallback<LocalFilter>() {
            @Override
            public boolean areItemsTheSame(@NonNull LocalFilter a, @NonNull LocalFilter b) {
                return a.id == b.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull LocalFilter a, @NonNull LocalFilter b) {
                return a.name.equals(b.name)
                    && a.signal.equals(b.signal)
                    && a.isRegex == b.isRegex
                    && a.tag.equals(b.tag)
                    && a.sendToServer == b.sendToServer
                    && a.enabled == b.enabled;
            }
        };
}
