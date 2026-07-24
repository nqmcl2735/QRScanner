package com.example.qrapp.ui.detail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.util.QRContentParser;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRDetailViewModel extends ViewModel {
    private final HistoryRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<QRHistoryItem> item = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> bitmap = new MutableLiveData<>();
    private final MutableLiveData<ParsedQRContent> parsedContent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);

    public QRDetailViewModel(HistoryRepository repository) {
        this.repository = repository;
    }

    public LiveData<QRHistoryItem> getItem() { return item; }
    public LiveData<Bitmap> getBitmap() { return bitmap; }
    public LiveData<ParsedQRContent> getParsedContent() { return parsedContent; }
    public LiveData<Boolean> getDeleted() { return deleted; }

    public void load(long id) {
        executor.execute(() -> {
            QRHistoryItem loaded = repository.getById(id);
            if (loaded == null) return;
            item.postValue(loaded);
            parsedContent.postValue(QRContentParser.parse(loaded.getContent()));
            if (loaded.getImagePath() != null) {
                bitmap.postValue(BitmapFactory.decodeFile(loaded.getImagePath()));
            }
        });
    }

    public void delete() {
        QRHistoryItem current = item.getValue();
        if (current == null) return;
        executor.execute(() -> {
            repository.delete(current);
            deleted.postValue(true);
        });
    }

    @Override protected void onCleared() {
        executor.shutdownNow();
    }
}
