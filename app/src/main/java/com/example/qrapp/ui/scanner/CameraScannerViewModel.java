package com.example.qrapp.ui.scanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.util.QRContentParser;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraScannerViewModel extends ViewModel {
    private final HistoryRepository historyRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Long> savedHistoryId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveFailed = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public CameraScannerViewModel(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public LiveData<Long> getSavedHistoryId() {
        return savedHistoryId;
    }

    public LiveData<Boolean> getSaveFailed() {
        return saveFailed;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void saveScanResult(String scannedText, String imagePath) {
        loading.setValue(true);
        executor.execute(() -> {
            try {
                ParsedQRContent parsed = QRContentParser.parse(scannedText);
                Bitmap bitmap = imagePath == null ? null : BitmapFactory.decodeFile(imagePath);
                long historyId = historyRepository.saveEntry(
                        scannedText, parsed.getType(), HistorySource.SCANNED, bitmap).getId();
                savedHistoryId.postValue(historyId);
            } catch (Exception exception) {
                saveFailed.postValue(true);
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void onNavigationHandled() {
        savedHistoryId.setValue(null);
    }

    public void onErrorHandled() {
        saveFailed.setValue(false);
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
