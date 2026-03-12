package com.personal.smsapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.personal.smsapp.databinding.ActivityComposeBinding;
import com.personal.smsapp.data.local.SmsRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComposeActivity extends AppCompatActivity {

    private ActivityComposeBinding binding;
    private final ExecutorService contactExecutor = Executors.newSingleThreadExecutor();
    private ContactSuggestionAdapter contactSuggestionAdapter;
    private boolean contactsLoaded;
    private boolean hasRequestedContactsPermission;

    private final ActivityResultLauncher<String> contactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadContactSuggestions();
                    if (binding.etRecipient.hasFocus()) {
                        binding.etRecipient.post(() -> {
                            if (binding.etRecipient.length() > 0) {
                                binding.etRecipient.showDropDown();
                            }
                        });
                    }
                } else {
                    Toast.makeText(this,
                            "Allow contacts access to get recipient suggestions.",
                            Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("New Message");
        }

        // Handle smsto: / sms: intent (from other apps)
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String number = data.getSchemeSpecificPart();
            if (number != null) binding.etRecipient.setText(number);
        }
        // Handle EXTRA_TEXT
        String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (extraText != null) binding.etMessage.setText(extraText);

        setupRecipientSuggestions();
        if (hasContactsPermission()) {
            loadContactSuggestions();
        }

        binding.btnSend.setOnClickListener(v -> send());
    }

    private void setupRecipientSuggestions() {
        contactSuggestionAdapter = new ContactSuggestionAdapter(this);
        binding.etRecipient.setAdapter(contactSuggestionAdapter);
        binding.etRecipient.setOnItemClickListener((parent, view, position, id) -> {
            ContactSuggestion suggestion = (ContactSuggestion) parent.getItemAtPosition(position);
            if (suggestion != null) {
                binding.etRecipient.setText(suggestion.number, false);
                binding.etRecipient.setSelection(suggestion.number.length());
                binding.etRecipient.dismissDropDown();
            }
        });
        binding.etRecipient.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ensureContactSuggestionsAvailable();
            }
        });
        binding.etRecipient.setOnClickListener(v -> ensureContactSuggestionsAvailable());
        binding.etRecipient.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    ensureContactSuggestionsAvailable();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void ensureContactSuggestionsAvailable() {
        if (hasContactsPermission()) {
            if (!contactsLoaded) {
                loadContactSuggestions();
            } else if (binding.etRecipient.length() > 0) {
                binding.etRecipient.post(() -> binding.etRecipient.showDropDown());
            }
            return;
        }

        if (!hasRequestedContactsPermission) {
            hasRequestedContactsPermission = true;
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void loadContactSuggestions() {
        if (contactsLoaded || !hasContactsPermission()) {
            return;
        }

        contactExecutor.execute(() -> {
            List<ContactSuggestion> suggestions = new ArrayList<>();
            Map<String, ContactSuggestion> dedupedContacts = new LinkedHashMap<>();
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            };

            try (Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC")) {

                if (cursor != null) {
                    int nameCol = cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
                    int numberCol = cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int normalizedNumberCol = cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);

                    while (cursor.moveToNext()) {
                        String number = numberCol >= 0 ? cursor.getString(numberCol) : null;
                        if (number == null || number.trim().isEmpty()) {
                            continue;
                        }

                        String displayName = nameCol >= 0 ? cursor.getString(nameCol) : null;
                        String normalizedNumber = normalizedNumberCol >= 0
                                ? cursor.getString(normalizedNumberCol)
                                : null;
                        String key = normalizedNumber;
                        if (key == null || key.trim().isEmpty()) {
                            key = number.replaceAll("[^0-9+]", "");
                        }
                        if (!dedupedContacts.containsKey(key)) {
                            dedupedContacts.put(key, new ContactSuggestion(displayName, number));
                        }
                    }
                }
            } catch (SecurityException e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Allow contacts access to get recipient suggestions.",
                        Toast.LENGTH_SHORT).show());
                return;
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Could not load contacts.",
                        Toast.LENGTH_SHORT).show());
                return;
            }

            suggestions.addAll(dedupedContacts.values());
            runOnUiThread(() -> {
                contactsLoaded = true;
                contactSuggestionAdapter.replaceItems(suggestions);
                if (binding.etRecipient.hasFocus() && binding.etRecipient.length() > 0) {
                    binding.etRecipient.showDropDown();
                }
            });
        });
    }

    private void send() {
        String recipient = binding.etRecipient.getText().toString().trim();
        String body      = binding.etMessage.getText().toString().trim();

        if (recipient.isEmpty()) {
            binding.etRecipient.setError("Enter a phone number");
            return;
        }
        if (body.isEmpty()) {
            binding.etMessage.setError("Enter a message");
            return;
        }

        try {
            SmsManager sms   = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(body);
            sms.sendMultipartTextMessage(recipient, null, parts, null, null);

            // Resolve the real thread ID for this recipient
            long threadId;
            try {
                threadId = android.provider.Telephony.Threads
                        .getOrCreateThreadId(this, recipient);
            } catch (Exception e) {
                threadId = 0;
            }
            final long finalThreadId = threadId;

            // Persist
            SmsRepository repo = SmsRepository.getInstance(this);
            repo.sendMessage(recipient, body, finalThreadId);

            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();

            // Open thread — pass the real thread ID so messages load correctly
            Intent open = new Intent(this, MessageThreadActivity.class);
            open.putExtra(MessageThreadActivity.EXTRA_ADDRESS,   recipient);
            open.putExtra(MessageThreadActivity.EXTRA_THREAD_ID, finalThreadId);
            startActivity(open);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        contactExecutor.shutdown();
    }

    private static class ContactSuggestion {
        private final String displayName;
        private final String number;
        private final String searchableText;
        private final String searchableNumber;

        ContactSuggestion(String displayName, String number) {
            this.displayName = displayName == null ? "" : displayName.trim();
            this.number = number.trim();
            this.searchableText = (this.displayName + " " + this.number).toLowerCase(Locale.getDefault());
            this.searchableNumber = this.number.replaceAll("[^0-9+]", "");
        }

        boolean matches(String query) {
            String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
            if (normalizedQuery.isEmpty()) {
                return true;
            }

            String numericQuery = normalizedQuery.replaceAll("[^0-9+]", "");
            return searchableText.contains(normalizedQuery)
                    || (!numericQuery.isEmpty() && searchableNumber.contains(numericQuery));
        }

        @Override
        public String toString() {
            return displayName.isEmpty() ? number : displayName + " • " + number;
        }
    }

    private static class ContactSuggestionAdapter extends ArrayAdapter<ContactSuggestion> {

        private final List<ContactSuggestion> allItems = new ArrayList<>();
        private final Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<ContactSuggestion> filteredItems = new ArrayList<>();
                String query = constraint == null ? "" : constraint.toString();

                for (ContactSuggestion item : allItems) {
                    if (item.matches(query)) {
                        filteredItems.add(item);
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredItems;
                results.count = filteredItems.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                if (results.values instanceof List<?>) {
                    List<?> values = (List<?>) results.values;
                    for (Object value : values) {
                        if (value instanceof ContactSuggestion) {
                            add((ContactSuggestion) value);
                        }
                    }
                }
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof ContactSuggestion) {
                    return ((ContactSuggestion) resultValue).number;
                }
                return super.convertResultToString(resultValue);
            }
        };

        ContactSuggestionAdapter(ComposeActivity activity) {
            super(activity, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        }

        void replaceItems(List<ContactSuggestion> items) {
            allItems.clear();
            allItems.addAll(items);
            clear();
            addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public Filter getFilter() {
            return filter;
        }
    }
}
