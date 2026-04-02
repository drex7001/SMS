package com.personal.smsapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.personal.smsapp.R;
import com.personal.smsapp.databinding.ActivitySettingsBinding;
import com.personal.smsapp.util.Prefs;
import com.personal.smsapp.worker.ApiSyncWorker;

import org.json.JSONArray;
import org.json.JSONException;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    private final ActivityResultLauncher<String> locationPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (!granted) {
                binding.switchWifiOnly.setChecked(false);
                Toast.makeText(this,
                    "Location permission needed to read WiFi network name",
                    Toast.LENGTH_LONG).show();
            }
        });

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
        setupWifiSection();
        setupFilterNav();
    }

    private void loadSettings() {
        binding.etApiUrl.setText(Prefs.getApiUrl(this));
        binding.etApiKey.setText(Prefs.getApiKey(this));
        binding.switchApiEnabled.setChecked(Prefs.isApiEnabled(this));
        binding.switchNotifications.setChecked(Prefs.areNotificationsEnabled(this));
        binding.switchWifiOnly.setChecked(Prefs.isWifiOnly(this));
        loadSsidChips();
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> {
            String  url             = binding.etApiUrl.getText().toString().trim();
            String  key             = binding.etApiKey.getText().toString().trim();
            boolean apiEnabled      = binding.switchApiEnabled.isChecked();
            boolean notificationsOn = binding.switchNotifications.isChecked();
            Prefs.saveAll(this, url, key, apiEnabled, notificationsOn);
            Prefs.setWifiOnly(this, binding.switchWifiOnly.isChecked());
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

    // ── WiFi ───────────────────────────────────────────────────────────────

    private void setupWifiSection() {
        binding.switchWifiOnly.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) requestLocationIfNeeded();
        });
        binding.btnAddSsid.setOnClickListener(v -> showAddSsidDialog());
    }

    private void requestLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("Location permission needed")
                    .setMessage("Android requires location permission to read the WiFi network name (SSID). This is only used to check if you're on your home network.")
                    .setPositiveButton("Grant", (d, w) -> locationPermLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION))
                    .setNegativeButton("Cancel", (d, w) -> binding.switchWifiOnly.setChecked(false))
                    .show();
            }
        }
    }

    private void showAddSsidDialog() {
        TextInputLayout til = new TextInputLayout(this);
        til.setHint("Network name (SSID)");
        til.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), 0);
        TextInputEditText et = new TextInputEditText(this);
        til.addView(et);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Add network")
            .setView(til)
            .setPositiveButton("Add", (d, w) -> {
                String ssid = et.getText() == null ? "" : et.getText().toString().trim();
                if (!ssid.isEmpty()) {
                    addSsidToPrefs(ssid);
                    addSsidChip(ssid);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadSsidChips() {
        binding.chipGroupSsids.removeAllViews();
        try {
            JSONArray arr = new JSONArray(Prefs.getHomeSsidsJson(this));
            for (int i = 0; i < arr.length(); i++) {
                addSsidChip(arr.getString(i));
            }
        } catch (JSONException ignored) {}
    }

    private void addSsidChip(String ssid) {
        Chip chip = new Chip(this);
        chip.setText(ssid);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            binding.chipGroupSsids.removeView(chip);
            removeSsidFromPrefs(ssid);
        });
        binding.chipGroupSsids.addView(chip);
    }

    private void addSsidToPrefs(String ssid) {
        try {
            JSONArray arr = new JSONArray(Prefs.getHomeSsidsJson(this));
            // Avoid duplicates
            for (int i = 0; i < arr.length(); i++) {
                if (ssid.equals(arr.getString(i))) return;
            }
            arr.put(ssid);
            Prefs.setHomeSsidsJson(this, arr.toString());
        } catch (JSONException ignored) {}
    }

    private void removeSsidFromPrefs(String ssid) {
        try {
            JSONArray old = new JSONArray(Prefs.getHomeSsidsJson(this));
            JSONArray updated = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                if (!ssid.equals(old.getString(i))) updated.put(old.getString(i));
            }
            Prefs.setHomeSsidsJson(this, updated.toString());
        } catch (JSONException ignored) {}
    }

    // ── Filter nav ─────────────────────────────────────────────────────────

    private void setupFilterNav() {
        binding.cardFilterRules.setOnClickListener(v ->
            startActivity(new Intent(this, FilterSettingsActivity.class)));
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
