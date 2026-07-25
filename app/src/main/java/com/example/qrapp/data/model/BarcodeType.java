package com.example.qrapp.data.model;

import com.google.zxing.BarcodeFormat;

public enum BarcodeType {
    QR_CODE("QR Code", BarcodeFormat.QR_CODE),
    EAN_13("EAN-13", BarcodeFormat.EAN_13),
    EAN_8("EAN-8", BarcodeFormat.EAN_8),
    UPC_A("UPC-A", BarcodeFormat.UPC_A),
    CODE_128("Code 128", BarcodeFormat.CODE_128),
    CODE_39("Code 39", BarcodeFormat.CODE_39),
    PDF_417("PDF417", BarcodeFormat.PDF_417),
    DATA_MATRIX("Data Matrix", BarcodeFormat.DATA_MATRIX),
    AZTEC("Aztec", BarcodeFormat.AZTEC);

    private final String displayName;
    private final BarcodeFormat zxingFormat;

    BarcodeType(String displayName, BarcodeFormat zxingFormat) {
        this.displayName = displayName;
        this.zxingFormat = zxingFormat;
    }

    public String getDisplayName() { return displayName; }
    public BarcodeFormat getZxingFormat() { return zxingFormat; }
}
