package com.personal.smsapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.personal.smsapp.data.local.Message;
import com.personal.smsapp.data.local.SmsRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageThreadViewModel extends AndroidViewModel {

    private final SmsRepository   repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MessageThreadViewModel(Application app) {
        super(app);
        repo = SmsRepository.getInstance(app);
    }

    public LiveData<List<Message>> getMessages(long threadId) {
        return repo.getMessages(threadId);
    }

    /** Resolves the thread_id for an address from the local DB (background only). */
    public long getThreadIdForAddress(String address) {
        return repo.getThreadIdForAddress(address);
    }

    public void markRead(long threadId) {
        executor.execute(() -> repo.markThreadRead(threadId));
    }

    public void saveOutgoing(String address, String body, long threadId) {
        executor.execute(() -> repo.sendMessage(address, body, threadId));
    }

    public void deleteConversation(long threadId) {
        executor.execute(() -> repo.deleteConversation(threadId));
    }

    public void deleteMessage(Message msg) {
        executor.execute(() -> repo.deleteMessage(msg));
    }
}
