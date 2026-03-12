package com.personal.smsapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.personal.smsapp.data.local.Conversation;
import com.personal.smsapp.data.local.SmsRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationListViewModel extends AndroidViewModel {

    private final SmsRepository                         repo;
    private final MutableLiveData<String>               searchQuery = new MutableLiveData<>("");
    private final LiveData<List<Conversation>>          conversations;
    private final ExecutorService                       executor = Executors.newSingleThreadExecutor();

    public ConversationListViewModel(Application app) {
        super(app);
        repo = SmsRepository.getInstance(app);

        // Switch between search results and full list based on query
        conversations = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return repo.getActiveConversations();
            } else {
                return repo.searchConversations(query.trim());
            }
        });
    }

    public LiveData<List<Conversation>> getConversations() {
        return conversations;
    }

    public void search(String query) {
        searchQuery.setValue(query);
    }

    public void clearSearch() {
        searchQuery.setValue("");
    }

    public void deleteConversation(long threadId) {
        executor.execute(() -> repo.deleteConversation(threadId));
    }
}
