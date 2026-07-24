package com.example.qrapp.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;
import com.example.qrapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Hiển thị lựa chọn chia sẻ (nội dung / ảnh QR) rồi mở menu chia sẻ mặc định của Android. */
public final class ShareUtil {
    private ShareUtil() {}

    public static void showShareChooser(Activity activity, String content, Bitmap bitmap) {
        String[] items = {
                activity.getString(R.string.share_content_option),
                activity.getString(R.string.share_image_option)
        };
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.share_action)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) shareText(activity, content);
                    else shareImage(activity, bitmap);
                })
                .show();
    }

    public static void shareText(Activity activity, String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, content);
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_action)));
    }

    public static void shareImage(Activity activity, Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(activity, R.string.share_image_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File folder = new File(activity.getCacheDir(), "qr_share");
            if (!folder.exists() && !folder.mkdirs()) throw new IOException("Không thể tạo thư mục tạm");
            File file = new File(folder, "qr_share.png");
            try (OutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProviderUtil.getUriForFile(activity, file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_action)));
        } catch (IOException exception) {
            Toast.makeText(activity, R.string.share_image_error, Toast.LENGTH_SHORT).show();
        }
    }
}
