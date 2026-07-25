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
    private final MutableLiveData<String> exportResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> importResult = new MutableLiveData<>();


    public HistoryViewModel(HistoryRepository repository) {
        this.repository = repository;
        loadHistory();
    }

    public LiveData<List<QRHistoryItem>> getItems() { return items; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getExportResult() { return exportResult; }
    public LiveData<Integer> getImportResult() { return importResult; }


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

    public void exportHistory() {
        loading.setValue(true);
        executor.execute(() -> {
            String csv = repository.exportToCsv();
            exportResult.postValue(csv);
            loading.postValue(false);
        });
    }

    public void importHistory(String csvContent) {
        loading.setValue(true);
        executor.execute(() -> {
            int count = repository.importFromCsv(csvContent);
            importResult.postValue(count);
            items.postValue(repository.getAll());
            loading.postValue(false);
        });
    }


    @Override protected void onCleared() {
        executor.shutdownNow();
    }
}
