package com.example.qrapp.util;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Lưu bản chụp ảnh QR (nội dung tạo/quét) vào bộ nhớ riêng của app để phục vụ lịch sử, chia sẻ và sao chép ảnh. */
public final class ImageFileStore {
    private static final String FOLDER_NAME = "qr_history";

    private ImageFileStore() {}

    public static File saveSnapshot(Context context, Bitmap bitmap) throws IOException {
        File folder = new File(context.getFilesDir(), FOLDER_NAME);
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Không thể tạo thư mục lưu lịch sử");
        String fileName = "qr_" + new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(new java.util.Date()) + ".png";
        File file = new File(folder, fileName);
        try (OutputStream stream = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IOException("Không thể ghi ảnh QR");
        }
        return file;
    }

    public static void delete(String path) {
        if (path == null) return;
        File file = new File(path);
        if (file.exists()) file.delete();
    }
}
