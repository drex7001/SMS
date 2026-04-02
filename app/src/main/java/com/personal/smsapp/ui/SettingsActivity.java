package com.personal.smsapp.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.personal.smsapp.databinding.ActivitySettingsBinding;
import com.personal.smsapp.util.Prefs;
import com.personal.smsapp.worker.ApiSyncWorker;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        loadSettings();
        setupSaveButton();
        setupTestButton();
    }

    private void loadSettings() {
        binding.etApiUrl.setText(Prefs.getApiUrl(this));
        binding.etApiKey.setText(Prefs.getApiKey(this));
        binding.switchApiEnabled.setChecked(Prefs.isApiEnabled(this));
        binding.switchNotifications.setChecked(Prefs.areNotificationsEnabled(this));
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> {
            String  url               = binding.etApiUrl.getText().toString().trim();
            String  key               = binding.etApiKey.getText().toString().trim();
            boolean apiEnabled        = binding.switchApiEnabled.isChecked();
            boolean notificationsOn   = binding.switchNotifications.isChecked();

            Prefs.saveAll(this, url, key, apiEnabled, notificationsOn);
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTestButton() {
        binding.btnTestSync.setOnClickListener(v -> {
            ApiSyncWorker.enqueue(this);
            Toast.makeText(this, "API sync triggered — check logcat for results",
                Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
