package com.personal.smsapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.personal.smsapp.R;
import com.personal.smsapp.data.local.SmsRepository;
import com.personal.smsapp.databinding.ActivityConversationListBinding;
import com.personal.smsapp.util.PhoneUtils;
import com.personal.smsapp.util.Prefs;

import java.util.ArrayList;

public class ConversationListActivity extends AppCompatActivity {

    private static final int REQUEST_DEFAULT_SMS = 42;

    private ActivityConversationListBinding binding;
    private ConversationListViewModel        viewModel;
    private ConversationAdapter              adapter;

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = !result.containsValue(false);
            if (allGranted) checkDefaultSmsApp();
            else showPermissionRationale();
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding   = ActivityConversationListBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        setupRecyclerView();
        setupSearch();
        setupFab();
        observeData();
        checkPermissions();
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter(conversation -> {
            Intent intent = new Intent(this, MessageThreadActivity.class);
            intent.putExtra(MessageThreadActivity.EXTRA_THREAD_ID, conversation.threadId);
            intent.putExtra(MessageThreadActivity.EXTRA_ADDRESS,   conversation.address);
            intent.putExtra(MessageThreadActivity.EXTRA_NAME,      conversation.displayName);
            startActivity(intent);
        }, conversation -> {
            // Long-press: show delete dialog
            new MaterialAlertDialogBuilder(this)
                .setTitle("Delete conversation")
                .setMessage("Delete all messages with " + conversation.displayName + "?")
                .setPositiveButton("Delete", (d, w) -> viewModel.deleteConversation(conversation.threadId))
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
    }

    private void setupSearch() {
        binding.searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int b, int count) {
                String q = s.toString().trim();
                if (q.isEmpty()) {
                    viewModel.clearSearch();
                } else {
                    viewModel.search(q);
                }
            }
        });
    }

    private void setupFab() {
        binding.fabCompose.setOnClickListener(v ->
            startActivity(new Intent(this, ComposeActivity.class)));
    }

    private void observeData() {
        viewModel.getConversations().observe(this, conversations -> {
            adapter.submitList(conversations);
            binding.emptyState.setVisibility(
                (conversations == null || conversations.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Permissions ────────────────────────────────────────────────────────

    private void checkPermissions() {
        String[] required = {
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
        };
        boolean allGranted = true;
        for (String p : required) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            checkDefaultSmsApp();
        } else {
            permissionLauncher.launch(required);
        }
    }

    private void checkDefaultSmsApp() {
        if (!PhoneUtils.isDefaultSmsApp(this)) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Set as default SMS app")
                .setMessage("To send and receive SMS, please set this as your default messaging app.")
                .setPositiveButton("Set as default", (d, w) ->
                    PhoneUtils.requestDefaultSmsApp(this, REQUEST_DEFAULT_SMS))
                .setNegativeButton("Not now", null)
                .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If we just became the default SMS app (e.g. via system settings), run sync
        if (PhoneUtils.isDefaultSmsApp(this) && !Prefs.isInitialSyncDone(this)) {
            runInitialSync();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEFAULT_SMS) {
            // Trust resultCode == RESULT_OK; isDefaultSmsApp() may lag on Android Q+
            if (resultCode == RESULT_OK) {
                runInitialSync();
            } else {
                Snackbar.make(binding.getRoot(),
                    "App is not set as default. Received SMS will not appear.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> checkDefaultSmsApp())
                    .show();
            }
        }
    }

    /**
     * One-time import of existing messages from the system SMS content provider.
     * Guarded by a SharedPreferences flag so it runs only once.
     */
    private void runInitialSync() {
        Prefs.setInitialSyncDone(this); // mark first so re-entrancy is safe
        SmsRepository.getInstance(this).syncFromSystem(() -> {
            // Callback is on a background thread – post UI update to main thread
            runOnUiThread(() ->
                Snackbar.make(binding.getRoot(),
                    "Messages synced.",
                    Snackbar.LENGTH_SHORT).show());
        });
    }

    private void showPermissionRationale() {
        Snackbar.make(binding.getRoot(),
            "SMS permissions are required to read and send messages.",
            Snackbar.LENGTH_LONG)
            .setAction("Retry", v -> checkPermissions())
            .show();
    }
}
