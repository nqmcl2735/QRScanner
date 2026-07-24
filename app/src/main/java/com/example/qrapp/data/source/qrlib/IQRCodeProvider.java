package com.example.qrapp.data.source.qrlib;

import android.graphics.Bitmap;

public interface IQRCodeProvider {
    Bitmap encode(String content, int width, int height) throws Exception;
    String decode(Bitmap bitmap) throws Exception;
}
