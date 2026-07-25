package com.example.qrapp.data.model;

import android.graphics.Bitmap;

public class ScanResult {
    private final String text;
    private final Bitmap bitmap;
    private final String format;

    public ScanResult(String text, Bitmap bitmap) {
        this(text, bitmap, "QR_CODE");
    }

    public ScanResult(String text, Bitmap bitmap, String format) {
        this.text = text;
        this.bitmap = bitmap;
        this.format = format;
    }

    public String getText() { return text; }
    public Bitmap getBitmap() { return bitmap; }
    public String getFormat() { return format; }
}
