package com.personal.smsapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.personal.smsapp.data.local.LocalFilter;
import com.personal.smsapp.data.local.SmsRepository;

import java.util.List;

public class FilterSettingsViewModel extends AndroidViewModel {

    private final SmsRepository repo;

    public FilterSettingsViewModel(Application app) {
        super(app);
        repo = SmsRepository.getInstance(app);
    }

    public LiveData<List<LocalFilter>> getFilters() {
        return repo.getFilters();
    }

    public void save(LocalFilter filter) {
        if (filter.id == 0) {
            repo.saveFilter(filter);
        } else {
            repo.updateFilter(filter);
        }
    }

    public void delete(LocalFilter filter) {
        repo.deleteFilter(filter);
    }

    public void replaceAll(List<LocalFilter> filters, Runnable onDone) {
        repo.replaceAllFilters(filters, onDone);
    }
}
