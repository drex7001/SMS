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

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(text);

            // Build sent/delivered intents
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                Intent si = new Intent("SMS_SENT");
                sentIntents.add(PendingIntent.getBroadcast(this, i, si,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }

            smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, null);

            // Persist to our DB
            viewModel.saveOutgoing(address, text, threadId);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        binding.btnSend.setEnabled(true);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_thread, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
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
