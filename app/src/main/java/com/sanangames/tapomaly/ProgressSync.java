package com.sanangames.tapomaly;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONObject;

public final class ProgressSync {

    public interface Done {
        void onFinished(boolean saved);
    }

    private ProgressSync() {
    }

    public static void pull(Context context) {
        if (!Session.isSignedIn(context)) {
            return;
        }
        StoreApi.me(context, new StoreApi.Result() {
            @Override
            public void onSuccess(Object data) {
                if (data instanceof JSONObject) {
                    JSONObject profile = (JSONObject) data;
                    Session.saveProfile(
                            context,
                            profile.optString("email", Session.email(context)),
                            profile.optString("nickname", Session.nickname(context))
                    );
                    Session.applyRemoteProgress(context, profile);
                }
                Session.flushPurchases(context);
            }

            @Override
            public void onError(String message) {
                // Local progress stays playable when the store profile cannot be read.
            }
        });
    }

    public static void save(Context context, boolean toastResult) {
        save(context, toastResult, null);
    }

    public static void save(Context context, boolean toastResult, Done done) {
        if (!Session.isSignedIn(context)) {
            if (toastResult) {
                Toast.makeText(context, "Sign in to save progress", Toast.LENGTH_SHORT).show();
            }
            if (done != null) {
                done.onFinished(false);
            }
            return;
        }

        StoreApi.saveProgress(
                context,
                Session.unlockedLevel(context),
                Session.bestScore(context),
                new StoreApi.Result() {
                    @Override
                    public void onSuccess(Object data) {
                        if (toastResult) {
                            Toast.makeText(context, "Progress saved", Toast.LENGTH_SHORT).show();
                        }
                        if (done != null) {
                            done.onFinished(true);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (toastResult) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }
                        if (done != null) {
                            done.onFinished(false);
                        }
                    }
                }
        );
    }
}
