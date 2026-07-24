package com.example.qrapp.data.source.storage;

import android.graphics.Bitmap;
import android.net.Uri;
import java.io.IOException;

public interface IStorageDataSource {
    Uri saveImage(Bitmap bitmap, String fileName) throws IOException;
    Bitmap loadImageAsBitmap(Uri uri) throws IOException;
}
