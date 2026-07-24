package com.example.qrapp.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.example.qrapp.R;
import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.model.QRContentType;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;

/** Sinh các nút hành động (Mở Maps / Lưu danh bạ / Gọi điện / Gửi email / Kết nối Wifi) tương ứng với loại nội dung QR đã nhận diện. */
public final class QRActionBinder {
    private QRActionBinder() {}

    public static void bind(Activity activity, LinearLayout container, ParsedQRContent parsed) {
        container.removeAllViews();
        if (parsed == null) {
            container.setVisibility(View.GONE);
            return;
        }
        switch (parsed.getType()) {
            case LOCATION:
                addButton(activity, container, parsed.getType(), R.string.action_open_maps, R.drawable.ic_location,
                        view -> openLocation(activity, parsed));
                break;
            case CONTACT:
                addButton(activity, container, parsed.getType(), R.string.action_save_contact, R.drawable.ic_contact,
                        view -> saveContact(activity, parsed));
                if (notEmpty(parsed.getContactPhone())) {
                    addButton(activity, container, parsed.getType(), R.string.action_call, R.drawable.ic_call,
                            view -> callPhone(activity, parsed.getContactPhone()));
                }
                break;
            case WIFI:
                addButton(activity, container, parsed.getType(), R.string.action_connect_wifi, R.drawable.ic_wifi,
                        view -> WifiConnectHelper.connect(activity, parsed.getSsid(), parsed.getPassword(), parsed.getSecurityType()));
                break;
            case EMAIL:
                addButton(activity, container, parsed.getType(), R.string.action_send_email, R.drawable.ic_email,
                        view -> sendEmail(activity, parsed));
                break;
            case PHONE:
                addButton(activity, container, parsed.getType(), R.string.action_call, R.drawable.ic_call,
                        view -> callPhone(activity, parsed.getContactPhone()));
                break;
            case SMS:
                addButton(activity, container, parsed.getType(), R.string.action_send_sms, R.drawable.ic_sms,
                        view -> sendSms(activity, parsed));
                break;
            case URL:
                addButton(activity, container, parsed.getType(), R.string.action_open_link, R.drawable.ic_link,
                        view -> openUrl(activity, parsed.getRawContent()));
                break;
            default:
                break;
        }
        container.setVisibility(container.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private static void addButton(Activity activity, LinearLayout container, QRContentType type,
                                   int textRes, int iconRes, View.OnClickListener listener) {
        MaterialButton button = (MaterialButton) LayoutInflater.from(activity)
                .inflate(R.layout.item_qr_action_button, container, false);
        button.setText(textRes);
        button.setIconResource(iconRes);
        int color = QRTypeStyle.color(activity, type);
        int softColor = QRTypeStyle.softColor(activity, type);
        button.setBackgroundTintList(ColorStateList.valueOf(softColor));
        button.setTextColor(color);
        button.setIconTint(ColorStateList.valueOf(color));
        button.setOnClickListener(listener);
        container.addView(button);
    }

    private static void openLocation(Activity activity, ParsedQRContent parsed) {
        Uri uri = Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f",
                parsed.getLatitude(), parsed.getLongitude(), parsed.getLatitude(), parsed.getLongitude()));
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(activity, R.string.no_maps_app, Toast.LENGTH_LONG).show();
        }
    }

    private static void saveContact(Activity activity, ParsedQRContent parsed) {
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        if (notEmpty(parsed.getContactName())) intent.putExtra(ContactsContract.Intents.Insert.NAME, parsed.getContactName());
        if (notEmpty(parsed.getContactPhone())) intent.putExtra(ContactsContract.Intents.Insert.PHONE, parsed.getContactPhone());
        if (notEmpty(parsed.getContactEmail())) intent.putExtra(ContactsContract.Intents.Insert.EMAIL, parsed.getContactEmail());
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(activity, R.string.no_contacts_app, Toast.LENGTH_LONG).show();
        }
    }

    private static void callPhone(Activity activity, String phone) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(activity, R.string.no_dialer_app, Toast.LENGTH_LONG).show();
        }
    }

    private static void sendEmail(Activity activity, ParsedQRContent parsed) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(parsed.getContactEmail())));
        if (notEmpty(parsed.getEmailSubject())) intent.putExtra(Intent.EXTRA_SUBJECT, parsed.getEmailSubject());
        if (notEmpty(parsed.getEmailBody())) intent.putExtra(Intent.EXTRA_TEXT, parsed.getEmailBody());
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(activity, R.string.no_email_app, Toast.LENGTH_LONG).show();
        }
    }

    private static void sendSms(Activity activity, ParsedQRContent parsed) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(parsed.getContactPhone())));
        if (notEmpty(parsed.getSmsBody())) intent.putExtra("sms_body", parsed.getSmsBody());
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(activity, R.string.no_sms_app, Toast.LENGTH_LONG).show();
        }
    }

    private static void openUrl(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(activity, R.string.no_browser_app, Toast.LENGTH_LONG).show();
        }
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
