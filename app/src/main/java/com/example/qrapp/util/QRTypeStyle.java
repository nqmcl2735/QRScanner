package com.example.qrapp.util;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.example.qrapp.R;
import com.example.qrapp.data.model.QRContentType;

/** Bảng tra cứu nhãn, màu sắc và biểu tượng hiển thị cho từng loại nội dung QR (dùng cho chip, icon danh sách, nút hành động). */
public final class QRTypeStyle {
    private QRTypeStyle() {}

    public static int labelRes(QRContentType type) {
        switch (type) {
            case WIFI: return R.string.type_label_wifi;
            case LOCATION: return R.string.type_label_location;
            case CONTACT: return R.string.type_label_contact;
            case EMAIL: return R.string.type_label_email;
            case PHONE: return R.string.type_label_phone;
            case SMS: return R.string.type_label_sms;
            case URL: return R.string.type_label_url;
            default: return R.string.type_label_text;
        }
    }

    public static int iconRes(QRContentType type) {
        switch (type) {
            case WIFI: return R.drawable.ic_wifi;
            case LOCATION: return R.drawable.ic_location;
            case CONTACT: return R.drawable.ic_contact;
            case EMAIL: return R.drawable.ic_email;
            case PHONE: return R.drawable.ic_call;
            case SMS: return R.drawable.ic_sms;
            case URL: return R.drawable.ic_link;
            default: return R.drawable.ic_qr;
        }
    }

    public static int colorRes(QRContentType type) {
        switch (type) {
            case WIFI: return R.color.accent_blue;
            case LOCATION: return R.color.accent_orange;
            case CONTACT: return R.color.accent_purple;
            case EMAIL: return R.color.accent_teal;
            case PHONE: return R.color.accent_rose;
            case SMS: return R.color.accent_indigo;
            case URL: return R.color.accent_sky;
            default: return R.color.text_secondary;
        }
    }

    public static int softColorRes(QRContentType type) {
        switch (type) {
            case WIFI: return R.color.accent_blue_soft;
            case LOCATION: return R.color.accent_orange_soft;
            case CONTACT: return R.color.accent_purple_soft;
            case EMAIL: return R.color.accent_teal_soft;
            case PHONE: return R.color.accent_rose_soft;
            case SMS: return R.color.accent_indigo_soft;
            case URL: return R.color.accent_sky_soft;
            default: return R.color.neutral_soft;
        }
    }

    public static int color(Context context, QRContentType type) {
        return ContextCompat.getColor(context, colorRes(type));
    }

    public static int softColor(Context context, QRContentType type) {
        return ContextCompat.getColor(context, softColorRes(type));
    }
}
