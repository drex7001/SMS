package com.personal.smsapp.ui;

import android.app.Application;
import android.util.Pair;

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

    public enum FilterType { ALL, UNREAD, PENDING, TAG }

    /** Holds both the search query AND the active filter tab */
    static final class QueryState {
        final String     search;
        final FilterType filter;
        final String     tag; // used when filter == TAG

        QueryState(String search, FilterType filter, String tag) {
            this.search = search;
            this.filter = filter;
            this.tag    = tag;
        }
    }

    private final SmsRepository                repo;
    private final MutableLiveData<QueryState>  state = new MutableLiveData<>(
        new QueryState("", FilterType.ALL, ""));
    private final LiveData<List<Conversation>> conversations;
    private final ExecutorService              executor = Executors.newSingleThreadExecutor();

    public ConversationListViewModel(Application app) {
        super(app);
        repo = SmsRepository.getInstance(app);

        conversations = Transformations.switchMap(state, s -> {
            if (s.search != null && !s.search.trim().isEmpty()) {
                return repo.searchConversations(s.search.trim());
            }
            switch (s.filter) {
                case UNREAD:  return repo.getUnreadConversations();
                case PENDING: return repo.getPendingSyncConversations();
                case TAG:     return repo.getConversationsByTag(s.tag);
                default:      return repo.getActiveConversations();
            }
        });
    }

    public LiveData<List<Conversation>> getConversations() { return conversations; }

    public LiveData<List<String>> getDistinctTags() { return repo.getDistinctTags(); }

    public void search(String query) {
        QueryState cur = state.getValue();
        FilterType f = cur != null ? cur.filter : FilterType.ALL;
        String t = cur != null ? cur.tag : "";
        state.setValue(new QueryState(query, f, t));
    }

    public void clearSearch() {
        QueryState cur = state.getValue();
        FilterType f = cur != null ? cur.filter : FilterType.ALL;
        String t = cur != null ? cur.tag : "";
        state.setValue(new QueryState("", f, t));
    }

    public void setFilter(FilterType filter, String tag) {
        QueryState cur = state.getValue();
        String q = cur != null ? cur.search : "";
        state.setValue(new QueryState(q, filter, tag != null ? tag : ""));
    }

    public void deleteConversation(long threadId) {
        executor.execute(() -> repo.deleteConversation(threadId));
    }
}
