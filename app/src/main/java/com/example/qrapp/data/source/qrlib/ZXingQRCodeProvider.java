package com.example.qrapp.data.source.qrlib;

import android.graphics.Bitmap;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.pdf417.encoder.Compaction;
import java.util.EnumMap;
import java.util.Map;

public class ZXingQRCodeProvider implements IQRCodeProvider {
    @Override
    public Bitmap encode(String content, int width, int height) throws Exception {
        return encode(content, width, height, BarcodeFormat.QR_CODE);
    }

    @Override
    public Bitmap encode(String content, int width, int height, BarcodeFormat format) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        if (format == BarcodeFormat.QR_CODE) {
            hints.put(EncodeHintType.MARGIN, 2);
        } else if (format == BarcodeFormat.PDF_417) {
            hints.put(EncodeHintType.PDF417_COMPACTION, Compaction.AUTO);
        }
        BitMatrix matrix = new MultiFormatWriter().encode(content, format, width, height, hints);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = matrix.get(x, y) ? 0xFF17211B : 0xFFFFFFFF;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    @Override
    public Result decode(Bitmap bitmap) throws Exception {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        return new MultiFormatReader().decode(binaryBitmap, hints);
    }
}
