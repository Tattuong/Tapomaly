package com.sanangames.tapomaly;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class StoreApi {

    public static final String BASE_URL = "https://store.blwsmartware.net/api/v1";
    public static final String GAME_CODE = "Tapomaly";

    public interface Result {
        void onSuccess(Object data);

        void onError(String message);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private StoreApi() {
    }

    public static void register(Context context, String email, String password, String nickname, Result result) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
            body.put("nickname", nickname);
        } catch (Exception error) {
            postError(result, "Could not build the request");
            return;
        }
        request(context, "POST", "/auth/register", body, false, false, result);
    }

    public static void login(Context context, String email, String password, Result result) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (Exception error) {
            postError(result, "Could not build the request");
            return;
        }
        request(context, "POST", "/auth/login", body, false, false, result);
    }

    public static void me(Context context, Result result) {
        request(context, "GET", "/auth/me", null, true, false, result);
    }

    public static void packs(Context context, Result result) {
        String path = "/games/" + urlEncode(GAME_CODE) + "/packs";
        request(context, "GET", path, null, false, false, result);
    }

    public static void saveProgress(Context context, int level, int score, Result result) {
        JSONObject body = new JSONObject();
        try {
            body.put("gameCode", GAME_CODE);
            body.put("score", Math.max(0, score));
            body.put("level", Math.max(1, level));
            JSONObject metadata = new JSONObject();
            metadata.put("unlockedLevel", Math.max(1, level));
            body.put("metadata", metadata);
        } catch (Exception error) {
            postError(result, "Could not build the request");
            return;
        }
        request(context, "POST", "/scores", body, true, false, result);
    }

    public static void verifyPurchase(
            Context context,
            String productId,
            String transactionId,
            String receipt,
            Result result
    ) {
        JSONObject body = new JSONObject();
        try {
            body.put("gameCode", GAME_CODE);
            body.put("productId", productId);
            body.put("transactionId", transactionId);
            body.put("platform", "ANDROID");
            body.put("receiptData", receipt);
        } catch (Exception error) {
            postError(result, "Could not build the request");
            return;
        }
        request(context, "POST", "/purchase/verify", body, true, false, result);
    }

    public static void logout(Context context, Result result) {
        JSONObject body = new JSONObject();
        try {
            body.put("refreshToken", Session.refreshToken(context));
        } catch (Exception error) {
            postError(result, "Could not build the request");
            return;
        }
        request(context, "POST", "/auth/logout", body, true, false, result);
    }

    private static void request(
            Context context,
            String method,
            String path,
            JSONObject body,
            boolean authorize,
            boolean retried,
            Result result
    ) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");
                if (authorize) {
                    String token = Session.accessToken(app);
                    if (token == null || token.isEmpty()) {
                        postError(result, "Sign in to continue");
                        return;
                    }
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
                if (body != null) {
                    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    OutputStream output = connection.getOutputStream();
                    output.write(bytes);
                    output.close();
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String response = stream == null ? "" : read(stream);

                if (code == 401 && authorize && !retried && Session.hasRefreshToken(app)) {
                    if (refresh(app)) {
                        request(app, method, path, body, true, true, result);
                        return;
                    }
                    Session.clear(app);
                    postError(result, "Sign in again");
                    return;
                }

                if (code < 200 || code >= 300) {
                    postError(result, errorMessage(response, code));
                    return;
                }

                Object data = parseData(response);
                MAIN.post(() -> {
                    if (result != null) {
                        result.onSuccess(data);
                    }
                });
            } catch (Exception error) {
                postError(result, "Could not reach the store");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, "tapomaly-store").start();
    }

    private static boolean refresh(Context context) {
        String refreshToken = Session.refreshToken(context);
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            JSONObject body = new JSONObject();
            body.put("refreshToken", refreshToken);
            connection = (HttpURLConnection) new URL(BASE_URL + "/auth/refresh").openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            OutputStream output = connection.getOutputStream();
            output.write(bytes);
            output.close();
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                return false;
            }
            Object data = parseData(read(connection.getInputStream()));
            if (!(data instanceof JSONObject)) {
                return false;
            }
            Session.saveTokens(context, (JSONObject) data);
            return true;
        } catch (Exception error) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Object parseData(String response) throws Exception {
        if (response == null || response.isEmpty()) {
            return new JSONObject();
        }
        JSONObject json = new JSONObject(response);
        if (!json.has("data")) {
            return json;
        }
        return json.get("data");
    }

    private static String errorMessage(String response, int code) {
        try {
            JSONObject json = new JSONObject(response);
            Object message = json.opt("message");
            if (message instanceof JSONArray) {
                JSONArray array = (JSONArray) message;
                if (array.length() > 0) {
                    return array.getString(0);
                }
            }
            if (message != null && !String.valueOf(message).isEmpty()) {
                return String.valueOf(message);
            }
        } catch (Exception ignored) {
            // Fall through to the status code.
        }
        if (code == 404) {
            return "Game not found";
        }
        return "Request failed (" + code + ")";
    }

    private static String read(InputStream stream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        stream.close();
        return output.toString("UTF-8");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void postError(Result result, String message) {
        MAIN.post(() -> {
            if (result != null) {
                result.onError(message);
            }
        });
    }
}
