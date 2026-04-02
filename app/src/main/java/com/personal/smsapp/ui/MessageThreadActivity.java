package com.personal.smsapp.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.personal.smsapp.R;
import com.personal.smsapp.databinding.ActivityMessageThreadBinding;
import com.personal.smsapp.util.NotificationHelper;
import com.personal.smsapp.util.PhoneUtils;

import java.util.ArrayList;

public class MessageThreadActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ID = "thread_id";
    public static final String EXTRA_ADDRESS   = "address";
    public static final String EXTRA_NAME      = "name";

    private ActivityMessageThreadBinding binding;
    private MessageThreadViewModel       viewModel;
    private MessageAdapter               adapter;

    private long   threadId;
    private String address;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding   = ActivityMessageThreadBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MessageThreadViewModel.class);
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        threadId = getIntent().getLongExtra(EXTRA_THREAD_ID, -1);
        address  = getIntent().getStringExtra(EXTRA_ADDRESS);
        name     = getIntent().getStringExtra(EXTRA_NAME);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(name != null && !name.equals(address) ? name : address);
            if (address != null) getSupportActionBar().setSubtitle(address);
        }

        setupRecyclerView();
        setupSendButton();
        observeMessages();
        setupEdgeToEdge();

        // Mark thread as read when opened
        if (threadId > 0) {
            viewModel.markRead(threadId);
            NotificationHelper.cancelForSender(this, address != null ? address : "");
        }
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        adapter.setOnMessageLongClickListener(msg ->
            new MaterialAlertDialogBuilder(this)
                .setTitle("Delete message")
                .setMessage("Delete this message?")
                .setPositiveButton("Delete", (d, w) -> viewModel.deleteMessage(msg))
                .setNegativeButton("Cancel", null)
                .show()
        );
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true); // newest at bottom
        binding.recyclerMessages.setLayoutManager(llm);
        binding.recyclerMessages.setAdapter(adapter);
    }

    private void setupSendButton() {
        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.etMessage.setOnEditorActionListener((tv, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (text.isEmpty() || address == null) return;

        binding.etMessage.setText("");
        binding.btnSend.setEnabled(false);

        SmsManager smsManager = getSystemService(SmsManager.class);
        ArrayList<String> parts = smsManager.divideMessage(text);

        // Build sent intents on main thread (PendingIntent requires UI context)
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            Intent si = new Intent("SMS_SENT");
            sentIntents.add(PendingIntent.getBroadcast(this, i, si,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }

        final String finalText = text;
        new Thread(() -> {
            try {
                smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, null);
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        viewModel.saveOutgoing(address, finalText, threadId);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        Toast.makeText(this, "Failed to send: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                        binding.btnSend.setEnabled(true);
                    }
                });
                return;
            }
            runOnUiThread(() -> {
                if (!isDestroyed()) binding.btnSend.setEnabled(true);
            });
        }).start();
    }

    private void observeMessages() {
        if (threadId < 0) return;
        viewModel.getMessages(threadId).observe(this, messages -> {
            adapter.submitList(messages);
            if (messages != null && !messages.isEmpty()) {
                binding.recyclerMessages.scrollToPosition(messages.size() - 1);
            }
        });
    }

    private void setupEdgeToEdge() {
        // Extend input bar behind nav bar and add matching bottom padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.inputBar, (v, insets) -> {
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int padV = Math.round(10 * v.getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), padV, v.getPaddingRight(), padV + navBottom);
            return insets;
        });
        // Keep message list clear of the taller input bar + nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerMessages, (v, insets) -> {
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int px88 = Math.round(88 * v.getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    px88 + navBottom);
            return insets;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_thread, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        if (id == R.id.action_delete_thread) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Delete conversation")
                .setMessage("This will delete all messages in this thread.")
                .setPositiveButton("Delete", (d, w) -> {
                    viewModel.deleteConversation(threadId);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        }
        if (id == R.id.action_call) {
            Intent call = new Intent(Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:" + address));
            startActivity(call);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
