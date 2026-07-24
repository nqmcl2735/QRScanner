package com.example.qrapp.util;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;

public final class FileProviderUtil {
    private FileProviderUtil() {}

    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context.getApplicationContext(),
                context.getPackageName() + ".fileprovider", file);
    }
}
