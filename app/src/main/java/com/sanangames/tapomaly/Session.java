package com.sanangames.tapomaly;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public final class Session {

    private static final String PREF_NAME = "TapomalyPrefs";
    private static final String ACCESS = "accessToken";
    private static final String REFRESH = "refreshToken";
    private static final String EMAIL = "accountEmail";
    private static final String NICKNAME = "accountNickname";
    private static final String PENDING = "pendingPurchases";
    public static final String BEST_SCORE = "bestScore";
    public static final String UNLOCKED_LEVEL = "unlockedLevel";

    private Session() {
    }

    public static boolean isSignedIn(Context context) {
        String token = accessToken(context);
        return token != null && !token.isEmpty();
    }

    public static String accessToken(Context context) {
        return prefs(context).getString(ACCESS, null);
    }

    public static String refreshToken(Context context) {
        return prefs(context).getString(REFRESH, null);
    }

    public static boolean hasRefreshToken(Context context) {
        String token = refreshToken(context);
        return token != null && !token.isEmpty();
    }

    public static String email(Context context) {
        return prefs(context).getString(EMAIL, "");
    }

    public static String nickname(Context context) {
        String nickname = prefs(context).getString(NICKNAME, "");
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        String email = email(context);
        int at = email.indexOf('@');
        if (at > 0) {
            return email.substring(0, at);
        }
        return "Player";
    }

    public static String label(Context context) {
        if (!isSignedIn(context)) {
            return "Sign in";
        }
        return nickname(context);
    }

    public static void saveTokens(Context context, JSONObject data) {
        prefs(context).edit()
                .putString(ACCESS, data.optString("accessToken", ""))
                .putString(REFRESH, data.optString("refreshToken", ""))
                .apply();
    }

    public static void saveProfile(Context context, String email, String nickname) {
        prefs(context).edit()
                .putString(EMAIL, email == null ? "" : email)
                .putString(NICKNAME, nickname == null ? "" : nickname)
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit()
                .remove(ACCESS)
                .remove(REFRESH)
                .remove(EMAIL)
                .remove(NICKNAME)
                .apply();
    }

    public static int unlockedLevel(Context context) {
        return prefs(context).getInt(UNLOCKED_LEVEL, 1);
    }

    public static int bestScore(Context context) {
        return prefs(context).getInt(BEST_SCORE, 0);
    }

    public static void rememberScore(Context context, int score) {
        if (score > bestScore(context)) {
            prefs(context).edit().putInt(BEST_SCORE, score).apply();
        }
    }

    public static void applyRemoteProgress(Context context, JSONObject profile) {
        JSONArray games = profile.optJSONArray("userGames");
        if (games == null) {
            return;
        }
        for (int index = 0; index < games.length(); index++) {
            JSONObject row = games.optJSONObject(index);
            if (row == null) {
                continue;
            }
            JSONObject game = row.optJSONObject("game");
            if (game == null || !StoreApi.GAME_CODE.equals(game.optString("code"))) {
                continue;
            }
            int remoteLevel = row.optInt("level", 1);
            int remoteScore = row.optInt("highScore", 0);
            SharedPreferences prefs = prefs(context);
            int localLevel = prefs.getInt(UNLOCKED_LEVEL, 1);
            int localScore = prefs.getInt(BEST_SCORE, 0);
            SharedPreferences.Editor editor = prefs.edit();
            if (remoteLevel > localLevel) {
                editor.putInt(UNLOCKED_LEVEL, remoteLevel);
            }
            if (remoteScore > localScore) {
                editor.putInt(BEST_SCORE, remoteScore);
            }
            editor.apply();
            return;
        }
    }

    public static void recordPurchase(Context context, String productId, String transactionId, String receipt) {
        recordPurchase(context, productId, transactionId, receipt, null);
    }

    public static void recordPurchase(
            Context context,
            String productId,
            String transactionId,
            String receipt,
            StoreApi.Result notify
    ) {
        int notifyIndex = -1;
        try {
            JSONArray pending = pending(context);
            JSONObject item = new JSONObject();
            item.put("productId", productId);
            item.put("transactionId", transactionId);
            item.put("receipt", receipt);
            pending.put(item);
            notifyIndex = pending.length() - 1;
            prefs(context).edit().putString(PENDING, pending.toString()).apply();
        } catch (Exception ignored) {
            // The Play purchase is already consumed. A missed queue entry can be restored from Play on the next billing sync.
        }
        if (!isSignedIn(context)) {
            if (notify != null) {
                notify.onError("Sign in to save this purchase");
            }
            return;
        }
        JSONArray queued = pending(context);
        if (queued.length() == 0) {
            if (notify != null) {
                notify.onError("Could not save this purchase");
            }
            return;
        }
        if (notifyIndex < 0) {
            notifyIndex = queued.length() - 1;
        }
        sendNext(context.getApplicationContext(), queued, 0, notifyIndex, notify);
    }

    public static void flushPurchases(Context context) {
        if (!isSignedIn(context)) {
            return;
        }
        JSONArray pending = pending(context);
        if (pending.length() == 0) {
            return;
        }
        sendNext(context.getApplicationContext(), pending, 0, -1, null);
    }

    private static void sendNext(
            Context context,
            JSONArray pending,
            int index,
            int notifyIndex,
            StoreApi.Result notify
    ) {
        if (index >= pending.length()) {
            prefs(context).edit().remove(PENDING).apply();
            return;
        }
        JSONObject item = pending.optJSONObject(index);
        if (item == null) {
            sendNext(context, pending, index + 1, notifyIndex, notify);
            return;
        }
        StoreApi.verifyPurchase(
                context,
                item.optString("productId"),
                item.optString("transactionId"),
                item.optString("receipt"),
                new StoreApi.Result() {
                    @Override
                    public void onSuccess(Object data) {
                        if (index == notifyIndex && notify != null) {
                            notify.onSuccess(data);
                        }
                        sendNext(context, pending, index + 1, notifyIndex, notify);
                    }

                    @Override
                    public void onError(String message) {
                        if (message != null && message.toLowerCase().contains("already")) {
                            if (index == notifyIndex && notify != null) {
                                notify.onSuccess(null);
                            }
                            sendNext(context, pending, index + 1, notifyIndex, notify);
                            return;
                        }
                        if (index == notifyIndex && notify != null) {
                            notify.onError(message);
                        }
                        keepFrom(context, pending, index);
                    }
                }
        );
    }

    private static void keepFrom(Context context, JSONArray pending, int start) {
        JSONArray rest = new JSONArray();
        for (int index = start; index < pending.length(); index++) {
            rest.put(pending.opt(index));
        }
        prefs(context).edit().putString(PENDING, rest.toString()).apply();
    }

    private static JSONArray pending(Context context) {
        try {
            return new JSONArray(prefs(context).getString(PENDING, "[]"));
        } catch (Exception error) {
            return new JSONArray();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
