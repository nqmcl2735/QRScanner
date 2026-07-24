package com.example.qrapp.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.QRContentType;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.data.source.history.IHistoryDataSource;
import com.example.qrapp.util.ImageFileStore;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class HistoryRepository {
    private final Context appContext;
    private final IHistoryDataSource dataSource;

    public HistoryRepository(Context context, IHistoryDataSource dataSource) {
        this.appContext = context.getApplicationContext();
        this.dataSource = dataSource;
    }

    /** Lưu ảnh QR + ghi 1 dòng lịch sử. Phải gọi trên background thread. */
    public QRHistoryItem saveEntry(String content, QRContentType type, HistorySource source, Bitmap bitmap) throws IOException {
        String imagePath = null;
        if (bitmap != null) {
            File file = ImageFileStore.saveSnapshot(appContext, bitmap);
            imagePath = file.getAbsolutePath();
        }
        QRHistoryItem item = new QRHistoryItem(0, content, type, source, System.currentTimeMillis(), imagePath);
        long id = dataSource.insert(item);
        item.setId(id);
        return item;
    }

    public List<QRHistoryItem> getAll() {
        return dataSource.getAll();
    }

    public QRHistoryItem getById(long id) {
        return dataSource.getById(id);
    }

    public void delete(QRHistoryItem item) {
        dataSource.delete(item.getId());
        ImageFileStore.delete(item.getImagePath());
    }
}
