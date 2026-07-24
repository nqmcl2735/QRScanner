package com.example.qrapp.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.data.repository.HistoryRepository;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryViewModel extends ViewModel {
    private final HistoryRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<QRHistoryItem>> items = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public HistoryViewModel(HistoryRepository repository) {
        this.repository = repository;
        loadHistory();
    }

    public LiveData<List<QRHistoryItem>> getItems() { return items; }
    public LiveData<Boolean> getLoading() { return loading; }

    public void loadHistory() {
        loading.setValue(true);
        executor.execute(() -> {
            items.postValue(repository.getAll());
            loading.postValue(false);
        });
    }

    public void deleteItem(QRHistoryItem item) {
        executor.execute(() -> {
            repository.delete(item);
            items.postValue(repository.getAll());
        });
    }

    @Override protected void onCleared() {
        executor.shutdownNow();
    }
}
