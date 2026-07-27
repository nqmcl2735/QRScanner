package com.example.qrapp.data.repository;

import android.graphics.Bitmap;
import android.net.Uri;
import com.example.qrapp.data.model.ScanResult;
import com.example.qrapp.data.source.qrlib.IQRCodeProvider;
import com.example.qrapp.data.source.storage.IStorageDataSource;

public class QRScannerRepository {
    private final IQRCodeProvider provider;
    private final IStorageDataSource storage;

    public QRScannerRepository(IQRCodeProvider provider, IStorageDataSource storage) {
        this.provider = provider;
        this.storage = storage;
    }

    public ScanResult decodeQRFromUri(Uri uri) throws Exception {
        Bitmap bitmap = storage.loadImageAsBitmap(uri);
        com.google.zxing.Result result = provider.decode(bitmap);
        return new ScanResult(result.getText(), bitmap, result.getBarcodeFormat().name());
    }
}
