package com.sanangames.tapomaly;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class IapConfig {

    public static final String CONFIG_URL = "https://api2.blwsmartware.net/N324.json";

    private static final String TAG = "TapomalyIap";
    private static final String PREF_NAME = "TapomalyPrefs";
    private static final String KEY_DISABLE = "iapDisable";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Listener {
        void onBillingAllowedChanged(boolean allowed);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile boolean resolved;
    private static volatile boolean billingAllowed;

    private IapConfig() {
    }

    public static void addListener(Listener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static boolean isResolved() {
        return resolved;
    }

    public static boolean isBillingAllowed() {
        return resolved && billingAllowed;
    }

    public static void fetch(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> fetchAttempt(app, 0), "tapomaly-iap-config").start();
    }

    private static void fetchAttempt(Context app, int attempt) {
        Integer remote = downloadDisable();
        if (remote == null && attempt < 1) {
            MAIN.postDelayed(() -> new Thread(
                    () -> fetchAttempt(app, attempt + 1),
                    "tapomaly-iap-config"
            ).start(), 2000);
            return;
        }
        MAIN.post(() -> apply(app, remote));
    }

    private static void apply(Context app, Integer remote) {
        SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int disable;
        if (remote != null) {
            disable = remote;
            prefs.edit().putInt(KEY_DISABLE, disable).apply();
        } else {
            disable = prefs.getInt(KEY_DISABLE, 0);
        }

        billingAllowed = disable != 1;
        resolved = true;
        Log.i(TAG, "disable=" + disable + " billingAllowed=" + billingAllowed);

        if (billingAllowed) {
            TapomalyApp.startBilling();
        } else {
            TapomalyApp.stopBilling();
        }

        for (Listener listener : LISTENERS) {
            listener.onBillingAllowedChanged(billingAllowed);
        }
    }

    private static Integer downloadDisable() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(CONFIG_URL).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                Log.w(TAG, "Config HTTP " + code);
                return null;
            }

            String body = readStream(connection.getInputStream());
            JSONObject json = new JSONObject(body);
            if (!json.has("disable")) {
                return 0;
            }
            return parseDisable(json.get("disable"));
        } catch (Exception error) {
            Log.w(TAG, "Config request failed: " + error.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static int parseDisable(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        if (raw instanceof Boolean) {
            return ((Boolean) raw) ? 1 : 0;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(text);
    }

    private static String readStream(InputStream stream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        stream.close();
        return output.toString("UTF-8");
    }
}
