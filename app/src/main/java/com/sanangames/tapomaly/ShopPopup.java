package com.sanangames.tapomaly;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopPopup {

    public interface Host {
        void playClick();

        void onShopClosed();
    }

    private ShopPopup() {
    }

    public static AlertDialog show(Activity activity, Host host) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }

        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 14));
        shell.setBackground(dialogBackground(activity));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText("Coins");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);

        TextView balance = new TextView(activity);
        balance.setText(CoinWallet.label(activity));
        balance.setTextColor(Color.parseColor("#FFD166"));
        balance.setTextSize(16);
        balance.setTypeface(null, Typeface.BOLD);
        balance.setGravity(Gravity.END);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(balance);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Buy a pack to add coins.");
        subtitle.setTextColor(Color.parseColor("#D8E2F3"));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(activity, 4), 0, dp(activity, 12));

        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        TextView status = new TextView(activity);
        status.setText("Loading packs...");
        status.setTextColor(Color.parseColor("#D8E2F3"));
        status.setTextSize(15);
        rows.addView(status);

        ScrollView scroll = new ScrollView(activity);
        scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        scroll.addView(rows);
        int scrollHeight = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.52f);

        TextView close = new TextView(activity);
        close.setText("Close");
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        close.setTypeface(null, Typeface.BOLD);
        close.setGravity(Gravity.CENTER);
        close.setBackground(secondary(activity));

        shell.addView(header);
        shell.addView(subtitle);
        CoinWallet.Listener coinListener = coins -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                balance.setText(coins + " coins");
            }
        };
        CoinWallet.addListener(coinListener);
        shell.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                scrollHeight
        ));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 46)
        );
        closeParams.topMargin = dp(activity, 10);
        shell.addView(close, closeParams);

        BillingManager.Listener productsListener = () -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                refreshPriceLabels(rows);
            }
        };
        IapConfig.Listener configListener = allowed -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                loadPacks(activity, host, rows, status);
            }
        };
        BillingManager billing = TapomalyApp.billing();
        if (billing != null) {
            billing.addListener(productsListener);
        }
        IapConfig.addListener(configListener);

        close.setOnClickListener(v -> {
            if (host != null) {
                host.playClick();
            }
            dialog.dismiss();
        });
        dialog.setView(shell);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(d -> {
            BillingManager current = TapomalyApp.billing();
            if (current != null) {
                current.removeListener(productsListener);
            }
            IapConfig.removeListener(configListener);
            CoinWallet.removeListener(coinListener);
            if (host != null) {
                host.onShopClosed();
            }
        });
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 28);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.72f);
        });
        dialog.show();
        loadPacks(activity, host, rows, status);
        return dialog;
    }

    private static void loadPacks(Activity activity, Host host, LinearLayout rows, TextView status) {
        StoreApi.packs(activity, new StoreApi.Result() {
            @Override
            public void onSuccess(Object data) {
                if (!(data instanceof JSONArray)) {
                    status.setText("No packs yet.");
                    return;
                }
                JSONArray packs = (JSONArray) data;
                bindPacks(activity, host, rows, status, packs);
            }

            @Override
            public void onError(String message) {
                status.setText(message);
            }
        });
    }

    private static void bindPacks(
            Activity activity,
            Host host,
            LinearLayout rows,
            TextView status,
            JSONArray packs
    ) {
        rows.removeAllViews();
        if (packs.length() == 0) {
            status.setText("No packs yet.");
            rows.addView(status);
            return;
        }

        List<String> ids = new ArrayList<>();
        boolean billingAllowed = IapConfig.isBillingAllowed();
        for (int index = 0; index < packs.length(); index++) {
            JSONObject pack = packs.optJSONObject(index);
            if (pack == null || !pack.optBoolean("isActive", true)) {
                continue;
            }
            String packId = pack.optString("packId", "");
            if (packId.isEmpty()) {
                continue;
            }
            ids.add(packId);
            rows.addView(packRow(activity, host, pack, billingAllowed));
            View spacer = new View(activity);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(activity, 8)
            ));
            rows.addView(spacer);
        }

        if (ids.isEmpty()) {
            status.setText("No packs yet.");
            rows.addView(status);
            return;
        }

        BillingManager billing = TapomalyApp.billing();
        if (billing != null) {
            billing.setProductIds(ids);
        }
    }

    private static View packRow(Activity activity, Host host, JSONObject pack, boolean billingAllowed) {
        String packId = pack.optString("packId");
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        row.setBackground(rowBackground(activity));

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);

        int coins = CoinWallet.coinsFor(packId);
        TextView name = new TextView(activity);
        name.setText(coins > 0 ? coins + " coins" : packId);
        name.setTextColor(Color.WHITE);
        name.setTextSize(16);
        name.setTypeface(null, Typeface.BOLD);

        TextView key = new TextView(activity);
        key.setText(packId);
        key.setTextColor(Color.parseColor("#8EA0C2"));
        key.setTextSize(12);

        TextView price = new TextView(activity);
        price.setTag(packId);
        price.setText(priceLabel(packId, pack));
        price.setTextColor(Color.parseColor("#FFD166"));
        price.setTextSize(14);
        price.setPadding(0, dp(activity, 4), 0, 0);

        copy.addView(name);
        copy.addView(key);
        copy.addView(price);
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (billingAllowed) {
            TextView buy = new TextView(activity);
            buy.setText("Buy");
            buy.setGravity(Gravity.CENTER);
            buy.setTextColor(Color.parseColor("#062033"));
            buy.setTextSize(15);
            buy.setTypeface(null, Typeface.BOLD);
            buy.setBackground(primary(activity));
            buy.setOnClickListener(v -> {
                if (host != null) {
                    host.playClick();
                }
                if (!IapConfig.isBillingAllowed()) {
                    return;
                }
                BillingManager billing = TapomalyApp.billing();
                if (billing != null && billing.isReady()) {
                    billing.launch(activity, packId);
                    return;
                }
                boolean debuggable = (activity.getApplicationInfo().flags
                        & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                if (debuggable && coins > 0) {
                    CoinWallet.add(activity, coins);
                    String transactionId = "debug-" + packId + "-" + System.currentTimeMillis();
                    Session.recordPurchase(
                            activity,
                            packId,
                            transactionId,
                            "debug-emulator",
                            new StoreApi.Result() {
                                @Override
                                public void onSuccess(Object data) {
                                    Toast.makeText(
                                            activity,
                                            "Purchase saved · +" + coins + " coins",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(
                                            activity,
                                            "+" + coins + " coins · " + message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );
                    return;
                }
                Toast.makeText(activity, "Google Play billing is unavailable", Toast.LENGTH_SHORT).show();
            });
            row.addView(buy, new LinearLayout.LayoutParams(dp(activity, 72), dp(activity, 40)));
        }
        return row;
    }

    private static void refreshPriceLabels(LinearLayout rows) {
        for (int index = 0; index < rows.getChildCount(); index++) {
            View child = rows.getChildAt(index);
            if (!(child instanceof LinearLayout)) {
                continue;
            }
            LinearLayout row = (LinearLayout) child;
            for (int inner = 0; inner < row.getChildCount(); inner++) {
                View view = row.getChildAt(inner);
                if (!(view instanceof LinearLayout)) {
                    continue;
                }
                LinearLayout copy = (LinearLayout) view;
                for (int textIndex = 0; textIndex < copy.getChildCount(); textIndex++) {
                    View text = copy.getChildAt(textIndex);
                    if (text instanceof TextView && text.getTag() instanceof String) {
                        String packId = (String) text.getTag();
                        BillingManager billing = TapomalyApp.billing();
                        String playPrice = billing == null ? null : billing.formattedPrice(packId);
                        if (playPrice != null) {
                            ((TextView) text).setText(playPrice);
                        }
                    }
                }
            }
        }
    }

    private static String priceLabel(String packId, JSONObject pack) {
        BillingManager billing = TapomalyApp.billing();
        String playPrice = billing == null ? null : billing.formattedPrice(packId);
        if (playPrice != null) {
            return playPrice;
        }
        double price = pack.optDouble("price", 0);
        String currency = pack.optString("currency", "USD");
        return String.format(Locale.US, "%s %.2f", currency, price);
    }

    private static GradientDrawable dialogBackground(Activity activity) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.parseColor("#20113F"),
                        Color.parseColor("#121A33"),
                        Color.parseColor("#061B3A")
                }
        );
        drawable.setCornerRadius(dp(activity, 26));
        drawable.setStroke(dp(activity, 2), Color.parseColor("#8B5CF6"));
        return drawable;
    }

    private static GradientDrawable rowBackground(Activity activity) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#1A2438"));
        drawable.setCornerRadius(dp(activity, 16));
        drawable.setStroke(dp(activity, 1), Color.parseColor("#33415C"));
        return drawable;
    }

    private static GradientDrawable primary(Activity activity) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.parseColor("#06D6A0"),
                        Color.parseColor("#00B4D8")
                }
        );
        drawable.setCornerRadius(dp(activity, 14));
        return drawable;
    }

    private static GradientDrawable secondary(Activity activity) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#26324A"));
        drawable.setCornerRadius(dp(activity, 14));
        drawable.setStroke(dp(activity, 1), Color.parseColor("#6D7EA8"));
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }
}
