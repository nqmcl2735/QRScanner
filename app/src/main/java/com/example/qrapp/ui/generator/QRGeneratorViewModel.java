package com.example.qrapp.ui.generator;

import android.graphics.Bitmap;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.repository.QRGeneratorRepository;
import com.example.qrapp.util.QRContentParser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.qrapp.data.model.BarcodeType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRGeneratorViewModel extends ViewModel {
    private final QRGeneratorRepository repository;
    private final HistoryRepository historyRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private final MutableLiveData<Uri> savedUri = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ParsedQRContent> parsedContent = new MutableLiveData<>();
    private final MutableLiveData<BarcodeType> selectedBarcodeType = new MutableLiveData<>(BarcodeType.QR_CODE);
    private Bitmap currentBitmap;

    public QRGeneratorViewModel(QRGeneratorRepository repository, HistoryRepository historyRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
    }

    public LiveData<Bitmap> getQrBitmap() { return qrBitmap; }
    public LiveData<Uri> getSavedUri() { return savedUri; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<ParsedQRContent> getParsedContent() { return parsedContent; }
    public LiveData<BarcodeType> getSelectedBarcodeType() { return selectedBarcodeType; }
    public Bitmap getCurrentBitmap() { return currentBitmap; }
    public void setBarcodeType(BarcodeType type) { selectedBarcodeType.setValue(type); }

    public void generateQRCode(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            error.setValue("Vui lòng nhập nội dung");
            return;
        }
        loading.setValue(true);
        executor.execute(() -> {
            try {
                BarcodeType type = selectedBarcodeType.getValue();
                if (type == null) type = BarcodeType.QR_CODE;
                int width = 1024;
                int height = 1024;
                if (type != BarcodeType.QR_CODE && type != BarcodeType.DATA_MATRIX && type != BarcodeType.AZTEC) {
                    width = 1024;
                    height = 300;
                }
                currentBitmap = repository.generateBarcodeBitmap(text, width, height, type.getZxingFormat());
                ParsedQRContent parsed = QRContentParser.parse(text);
                qrBitmap.postValue(currentBitmap);
                parsedContent.postValue(parsed);
                saveToHistory(text, parsed, currentBitmap);
            } catch (Exception exception) {
                error.postValue("Không thể tạo mã QR. Vui lòng thử lại.");
            } finally {
                loading.postValue(false);
            }
        });
    }

    private void saveToHistory(String text, ParsedQRContent parsed, Bitmap bitmap) {
        if (historyRepository == null) return;
        try {
            historyRepository.saveEntry(text, parsed.getType(), HistorySource.GENERATED, bitmap);
        } catch (Exception ignored) {
            // Lưu lịch sử là tác vụ phụ, không chặn luồng tạo mã QR khi thất bại.
        }
    }

    public void saveQRCodeToStorage() {
        if (currentBitmap == null) {
            error.setValue("Hãy tạo mã QR trước khi lưu");
            return;
        }
        loading.setValue(true);
        executor.execute(() -> {
            try {
                String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                savedUri.postValue(repository.saveBitmapToExternalStorage(currentBitmap, "QR_" + time + ".png"));
            } catch (Exception exception) {
                error.postValue("Không thể lưu ảnh. Hãy kiểm tra dung lượng và quyền truy cập.");
            } finally {
                loading.postValue(false);
            }
        });
    }

    @Override protected void onCleared() {
        executor.shutdownNow();
    }
}
