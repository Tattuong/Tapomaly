package com.sanangames.tapomaly;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CoinWallet {

    public interface Listener {
        void onCoinsChanged(int coins);
    }

    private static final String PREF_NAME = "TapomalyPrefs";
    private static final String KEY_COINS = "coins";
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private CoinWallet() {
    }

    public static void addListener(Listener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static int coinsFor(String packId) {
        if (packId == null) {
            return 0;
        }
        switch (packId) {
            case "tap_pack1":
                return 100;
            case "tap_pack2":
                return 250;
            case "tap_pack3":
                return 500;
            case "tap_pack4":
                return 800;
            case "tap_pack5":
                return 1200;
            case "tap_pack6":
                return 1800;
            case "tap_pack7":
                return 2600;
            case "tap_pack8":
                return 4000;
            case "tap_pack9":
                return 6500;
            case "tap_pack10":
                return 10000;
            default:
                return 0;
        }
    }

    public static int get(Context context) {
        return prefs(context).getInt(KEY_COINS, 0);
    }

    public static void add(Context context, int amount) {
        if (amount <= 0) {
            return;
        }
        int next = get(context) + amount;
        prefs(context).edit().putInt(KEY_COINS, next).apply();
        for (Listener listener : LISTENERS) {
            listener.onCoinsChanged(next);
        }
    }

    public static String label(Context context) {
        return get(context) + " coins";
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
