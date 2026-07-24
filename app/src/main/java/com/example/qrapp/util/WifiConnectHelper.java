package com.example.qrapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import com.example.qrapp.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Kết nối tới mạng Wifi đọc được từ mã QR, tương thích nhiều phiên bản Android. */
public final class WifiConnectHelper {
    private WifiConnectHelper() {}

    public static void connect(Activity activity, String ssid, String password, String securityType) {
        if (ssid == null || ssid.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            connectViaSettingsPanel(activity, ssid, password, securityType);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectViaSuggestion(activity, ssid, password, securityType);
        } else {
            connectLegacy(activity, ssid, password, securityType);
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static void connectViaSettingsPanel(Activity activity, String ssid, String password, String securityType) {
        ArrayList<WifiNetworkSuggestion> list = new ArrayList<>();
        list.add(buildSuggestion(ssid, password, securityType));
        Intent intent = new Intent(Settings.ACTION_WIFI_ADD_NETWORKS);
        intent.putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, list);
        activity.startActivity(intent);
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static void connectViaSuggestion(Activity activity, String ssid, String password, String securityType) {
        List<WifiNetworkSuggestion> suggestions = Collections.singletonList(buildSuggestion(ssid, password, securityType));
        WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int status = wifiManager == null ? -1 : wifiManager.addNetworkSuggestions(suggestions);
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(activity, R.string.wifi_suggestion_added, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity, R.string.wifi_connect_error, Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static WifiNetworkSuggestion buildSuggestion(String ssid, String password, String securityType) {
        WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder().setSsid(ssid);
        if (isWpa(securityType) && password != null && !password.isEmpty()) {
            builder.setWpa2Passphrase(password);
        }
        return builder.build();
    }

    @SuppressWarnings("deprecation")
    private static void connectLegacy(Activity activity, String ssid, String password, String securityType) {
        WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) return;
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        if (isWpa(securityType) && password != null && !password.isEmpty()) {
            config.preSharedKey = "\"" + password + "\"";
        } else if (isWep(securityType) && password != null && !password.isEmpty()) {
            config.wepKeys[0] = "\"" + password + "\"";
            config.wepTxKeyIndex = 0;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        int networkId = wifiManager.addNetwork(config);
        if (networkId == -1) {
            Toast.makeText(activity, R.string.wifi_connect_error, Toast.LENGTH_LONG).show();
            return;
        }
        wifiManager.disconnect();
        wifiManager.enableNetwork(networkId, true);
        wifiManager.reconnect();
        Toast.makeText(activity, R.string.wifi_connecting, Toast.LENGTH_LONG).show();
    }

    private static boolean isWpa(String securityType) {
        return securityType != null && securityType.toUpperCase(Locale.US).startsWith("WPA");
    }

    private static boolean isWep(String securityType) {
        return securityType != null && securityType.equalsIgnoreCase("WEP");
    }
}
