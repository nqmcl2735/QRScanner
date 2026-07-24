package com.example.qrapp.data.model;

import android.graphics.Bitmap;

public class ScanResult {
    private final String text;
    private final Bitmap bitmap;

    public ScanResult(String text, Bitmap bitmap) {
        this.text = text;
        this.bitmap = bitmap;
    }

    public String getText() { return text; }
    public Bitmap getBitmap() { return bitmap; }
}
