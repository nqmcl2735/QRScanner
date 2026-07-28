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

    private final MutableLiveData<String> message = new MutableLiveData<>();
    public LiveData<String> getMessage() { return message; }

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

    public void deleteAllHistory() {
        executor.execute(() -> {
            repository.deleteAll();
            items.postValue(repository.getAll());
            message.postValue("Đã xoá toàn bộ lịch sử");
        });
    }
    
    public void exportHistory(android.net.Uri uri, android.content.ContentResolver resolver) {
        loading.setValue(true);
        executor.execute(() -> {
            try {
                List<QRHistoryItem> list = repository.getAll();
                try (java.io.OutputStream os = resolver.openOutputStream(uri);
                     java.io.PrintWriter pw = new java.io.PrintWriter(os)) {
                    pw.println("id,content,type,source,timestamp,imagePath");
                    for (QRHistoryItem item : list) {
                        pw.printf("%d,\"%s\",%s,%s,%d,%s\n",
                                item.getId(),
                                item.getContent().replace("\"", "\"\""),
                                item.getType().name(),
                                item.getSource().name(),
                                item.getTimestamp(),
                                item.getImagePath() == null ? "" : item.getImagePath());
                    }
                }
                message.postValue("Đã xuất lịch sử thành công");
            } catch (Exception e) {
                message.postValue("Lỗi khi xuất file: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void importHistory(android.net.Uri uri, android.content.ContentResolver resolver) {
        loading.setValue(true);
        executor.execute(() -> {
            try {
                int count = 0;
                try (java.io.InputStream is = resolver.openInputStream(uri);
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                    String line = reader.readLine(); // skip header
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length >= 6) {
                            String content = parts[1].replaceAll("^\"|\"$", "").replace("\"\"", "\"");
                            com.example.qrapp.data.model.QRContentType type = com.example.qrapp.data.model.QRContentType.valueOf(parts[2]);
                            com.example.qrapp.data.model.HistorySource source = com.example.qrapp.data.model.HistorySource.valueOf(parts[3]);
                            
                            repository.saveEntry(content, type, source, null); 
                            count++;
                        }
                    }
                }
                items.postValue(repository.getAll()); // reload
                message.postValue("Đã nhập " + count + " mục lịch sử");
            } catch (Exception e) {
                message.postValue("Không thể nhập dữ liệu. Vui lòng kiểm tra định dạng file.");
            } finally {
                loading.postValue(false);
            }
        });
    }

    @Override protected void onCleared() {
        executor.shutdownNow();
    }
}
