package com.example.qrapp.ui.scanner;

import android.graphics.Bitmap;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.model.ScanResult;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.repository.QRScannerRepository;
import com.example.qrapp.util.QRContentParser;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerViewModel extends ViewModel {
    private final QRScannerRepository repository;
    private final HistoryRepository historyRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Uri> selectedImageUri = new MutableLiveData<>();
    private final MutableLiveData<String> decodedText = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ParsedQRContent> parsedContent = new MutableLiveData<>();
    private Bitmap currentBitmap;

    public QRScannerViewModel(QRScannerRepository repository, HistoryRepository historyRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
    }

    public LiveData<Uri> getSelectedImageUri() { return selectedImageUri; }
    public LiveData<String> getDecodedText() { return decodedText; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<ParsedQRContent> getParsedContent() { return parsedContent; }
    public Bitmap getCurrentBitmap() { return currentBitmap; }

    public void setSelectedImage(Uri uri) {
        selectedImageUri.setValue(uri);
        decodedText.setValue("");
        parsedContent.setValue(null);
        currentBitmap = null;
    }

    public void scanSelectedImage() {
        Uri uri = selectedImageUri.getValue();
        if (uri == null) {
            error.setValue("Vui lòng chọn một ảnh trước");
            return;
        }
        loading.setValue(true);
        executor.execute(() -> {
            try {
                ScanResult result = repository.decodeQRFromUri(uri);
                currentBitmap = result.getBitmap();
                ParsedQRContent parsed = QRContentParser.parse(result.getText());
                decodedText.postValue(result.getText());
                parsedContent.postValue(parsed);
                saveToHistory(result.getText(), parsed, result.getBitmap());
            } catch (Exception exception) {
                error.postValue("Không tìm thấy mã QR hợp lệ trong ảnh");
            } finally {
                loading.postValue(false);
            }
        });
    }

    private void saveToHistory(String text, ParsedQRContent parsed, Bitmap bitmap) {
        if (historyRepository == null) return;
        try {
            historyRepository.saveEntry(text, parsed.getType(), HistorySource.SCANNED, bitmap);
        } catch (Exception ignored) {
            // Lưu lịch sử là tác vụ phụ, không chặn luồng quét mã QR khi thất bại.
        }
    }

    @Override protected void onCleared() {
        executor.shutdownNow();
    }
}
