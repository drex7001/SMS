package com.personal.smsapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.personal.smsapp.R;
import com.personal.smsapp.data.local.LocalFilter;
import com.personal.smsapp.data.remote.ApiService;
import com.personal.smsapp.databinding.ActivityFilterSettingsBinding;
import com.personal.smsapp.databinding.DialogEditFilterBinding;
import com.personal.smsapp.util.DefaultFilters;
import com.personal.smsapp.util.Prefs;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FilterSettingsActivity extends AppCompatActivity {

    private ActivityFilterSettingsBinding binding;
    private FilterSettingsViewModel viewModel;
    private FilterRuleAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding   = ActivityFilterSettingsBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(FilterSettingsViewModel.class);
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupRecyclerView();
        observeFilters();

        binding.fabAdd.setOnClickListener(v -> showEditDialog(null));
    }

    private void setupRecyclerView() {
        adapter = new FilterRuleAdapter(new FilterRuleAdapter.Listener() {
            @Override public void onEdit(LocalFilter f)   { showEditDialog(f); }
            @Override public void onDelete(LocalFilter f) { confirmDelete(f); }
            @Override public void onToggleEnabled(LocalFilter f, boolean enabled) {
                f.enabled = enabled;
                viewModel.save(f);
            }
        });
        binding.recyclerFilters.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFilters.setAdapter(adapter);
    }

    private void observeFilters() {
        viewModel.getFilters().observe(this, filters -> {
            adapter.submitList(filters);
            binding.emptyState.setVisibility(
                (filters == null || filters.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    private void showEditDialog(LocalFilter existing) {
        DialogEditFilterBinding db = DialogEditFilterBinding.inflate(LayoutInflater.from(this));

        if (existing != null) {
            db.etName.setText(existing.name);
            db.etSignal.setText(existing.signal);
            db.switchRegex.setChecked(existing.isRegex);
            db.etTag.setText(existing.tag);
            db.switchSendToServer.setChecked(existing.sendToServer);
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle(existing == null ? "Add rule" : "Edit rule")
            .setView(db.getRoot())
            .setPositiveButton("Save", (d, w) -> {
                LocalFilter f = existing != null ? existing : new LocalFilter();
                f.name         = db.etName.getText().toString().trim();
                f.signal       = db.etSignal.getText().toString().trim();
                f.isRegex      = db.switchRegex.isChecked();
                f.tag          = db.etTag.getText().toString().trim();
                f.sendToServer = db.switchSendToServer.isChecked();
                if (f.signal.isEmpty()) {
                    Snackbar.make(binding.getRoot(), "Signal cannot be empty", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                viewModel.save(f);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmDelete(LocalFilter f) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete rule")
            .setMessage("Delete \"" + (f.name.isEmpty() ? f.signal : f.name) + "\"?")
            .setPositiveButton("Delete", (d, w) -> viewModel.delete(f))
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_filter_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        if (id == R.id.action_seed_defaults) {
            doSeedDefaults();
            return true;
        }
        if (id == R.id.action_backup) {
            doBackup();
            return true;
        }
        if (id == R.id.action_restore) {
            doRestore();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doSeedDefaults() {
        List<LocalFilter> defaults = DefaultFilters.get();
        int existing = adapter.getCurrentList().size();
        String msg = existing == 0
            ? "Add " + defaults.size() + " default filter rules?"
            : "Append " + defaults.size() + " default rules to your " + existing + " existing rules?";

        new MaterialAlertDialogBuilder(this)
            .setTitle("Default rules")
            .setMessage(msg)
            .setPositiveButton("Add", (d, w) -> {
                for (LocalFilter f : defaults) viewModel.save(f);
                Snackbar.make(binding.getRoot(),
                    "Added " + defaults.size() + " default rules",
                    Snackbar.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void doBackup() {
        List<LocalFilter> current = adapter.getCurrentList();
        if (current.isEmpty()) {
            Snackbar.make(binding.getRoot(), "No rules to backup", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String apiUrl = Prefs.getApiUrl(this);
        String apiKey = Prefs.getApiKey(this);
        String derivedUrl = ApiService.deriveFiltersUrl(apiUrl);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Backup rules")
            .setMessage("Will POST " + current.size() + " rules to:\n" + derivedUrl)
            .setPositiveButton("Backup", (d, w) -> executor.execute(() -> {
                ApiService api = new ApiService(apiUrl, apiKey);
                boolean ok = api.backupFilters(current);
                runOnUiThread(() -> Snackbar.make(binding.getRoot(),
                    ok ? "Backup successful" : "Backup failed — check server",
                    Snackbar.LENGTH_LONG).show());
            }))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void doRestore() {
        String apiUrl = Prefs.getApiUrl(this);
        String apiKey = Prefs.getApiKey(this);
        String derivedUrl = ApiService.deriveFiltersUrl(apiUrl);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Restore rules")
            .setMessage("Will replace all local rules with data from:\n" + derivedUrl)
            .setPositiveButton("Restore", (d, w) -> executor.execute(() -> {
                ApiService api = new ApiService(apiUrl, apiKey);
                List<LocalFilter> fetched = api.restoreFilters();
                if (fetched == null) {
                    runOnUiThread(() -> Snackbar.make(binding.getRoot(),
                        "Restore failed — check server", Snackbar.LENGTH_LONG).show());
                    return;
                }
                viewModel.replaceAll(fetched, () ->
                    runOnUiThread(() -> Snackbar.make(binding.getRoot(),
                        "Restored " + fetched.size() + " rules", Snackbar.LENGTH_SHORT).show()));
            }))
            .setNegativeButton("Cancel", null)
            .show();
    }
}
