package com.example.qrapp.data.repository;

import android.graphics.Bitmap;
import android.net.Uri;
import com.example.qrapp.data.source.qrlib.IQRCodeProvider;
import com.example.qrapp.data.source.storage.IStorageDataSource;
import java.io.IOException;

public class QRGeneratorRepository {
    private final IQRCodeProvider provider;
    private final IStorageDataSource storage;

    public QRGeneratorRepository(IQRCodeProvider provider, IStorageDataSource storage) {
        this.provider = provider;
        this.storage = storage;
    }

    public Bitmap generateQRBitmap(String content, int size) throws Exception {
        return provider.encode(content, size, size);
    }

    public Bitmap generateBarcodeBitmap(String content, int width, int height, com.google.zxing.BarcodeFormat format) throws Exception {
        return provider.encode(content, width, height, format);
    }

    public Uri saveBitmapToExternalStorage(Bitmap bitmap, String fileName) throws IOException {
        return storage.saveImage(bitmap, fileName);
    }
}
