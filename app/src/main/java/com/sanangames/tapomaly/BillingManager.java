package com.sanangames.tapomaly;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BillingManager implements com.android.billingclient.api.PurchasesUpdatedListener {

    public interface Listener {
        void onProductsChanged();
    }

    private static final String TAG = "TapomalyIap";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final Context appContext;
    private final Map<String, ProductDetails> products = new HashMap<>();
    private final Set<String> consuming = new HashSet<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private BillingClient billingClient;
    private int reconnects;
    private boolean connecting;
    private boolean productsQueried;
    private final List<String> requestedIds = new ArrayList<>();

    public BillingManager(Context context) {
        appContext = context.getApplicationContext();
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (!IapConfig.isBillingAllowed()) {
            return;
        }
        if (billingClient != null && billingClient.isReady()) {
            return;
        }
        if (connecting) {
            return;
        }

        connecting = true;
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();
        connect();
    }

    public void end() {
        connecting = false;
        productsQueried = false;
        products.clear();
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
        notifyProductsChanged();
    }

    @Nullable
    public String formattedPrice(String productId) {
        ProductDetails details = products.get(productId);
        if (details == null || details.getOneTimePurchaseOfferDetails() == null) {
            return null;
        }
        return details.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    public boolean hasProduct(String productId) {
        return products.containsKey(productId);
    }

    public boolean isReady() {
        return billingClient != null && billingClient.isReady();
    }

    public boolean productsQueried() {
        return productsQueried;
    }

    public void setProductIds(List<String> ids) {
        requestedIds.clear();
        if (ids != null) {
            requestedIds.addAll(ids);
        }
        queryProducts();
    }

    public void launch(Activity activity, String productId) {
        if (!IapConfig.isBillingAllowed() || activity == null || activity.isFinishing()) {
            return;
        }
        if (billingClient == null || !billingClient.isReady()) {
            toast("Google Play billing is unavailable");
            return;
        }

        ProductDetails details = products.get(productId);
        if (details == null) {
            toast("This pack is not available yet");
            return;
        }

        BillingFlowParams.ProductDetailsParams product = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build();
        BillingFlowParams flow = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(product))
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, flow);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            toast("Could not open Google Play");
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (!IapConfig.isBillingAllowed()) {
            return;
        }

        int code = billingResult.getResponseCode();
        if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
            return;
        }
        if (code != BillingClient.BillingResponseCode.OK || purchases == null) {
            toast("Purchase failed");
            return;
        }

        for (Purchase purchase : purchases) {
            handlePurchase(purchase);
        }
    }

    private void connect() {
        if (billingClient == null) {
            connecting = false;
            return;
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                connecting = false;
                if (!IapConfig.isBillingAllowed() || billingClient == null) {
                    return;
                }
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Billing setup " + billingResult.getDebugMessage());
                    productsQueried = true;
                    notifyProductsChanged();
                    return;
                }
                reconnects = 0;
                queryProducts();
                queryOwned();
            }

            @Override
            public void onBillingServiceDisconnected() {
                connecting = false;
                if (!IapConfig.isBillingAllowed() || billingClient == null || reconnects >= 3) {
                    return;
                }
                reconnects++;
                MAIN.postDelayed(() -> {
                    if (billingClient != null && IapConfig.isBillingAllowed()) {
                        connect();
                    }
                }, 1500);
            }
        });
    }

    private void queryProducts() {
        if (billingClient == null || !billingClient.isReady()) {
            return;
        }

        if (requestedIds.isEmpty()) {
            return;
        }

        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        for (String productId : requestedIds) {
            list.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(list)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> MAIN.post(() -> {
            products.clear();
            List<ProductDetails> productDetailsList = productDetails(billingResult, result);
            for (ProductDetails details : productDetailsList) {
                products.put(details.getProductId(), details);
            }
            productsQueried = true;
            Log.i(TAG, "Play products loaded: " + products.size());
            notifyProductsChanged();
        }));
    }

    private static List<ProductDetails> productDetails(BillingResult billingResult, QueryProductDetailsResult result) {
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                || result == null
                || result.getProductDetailsList() == null) {
            return Collections.emptyList();
        }
        return result.getProductDetailsList();
    }

    private void queryOwned() {
        if (billingClient == null || !billingClient.isReady() || !IapConfig.isBillingAllowed()) {
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK || purchases == null) {
                return;
            }
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            toast("Purchase is pending");
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            return;
        }
        if (!IapConfig.isBillingAllowed() || billingClient == null) {
            return;
        }

        String token = purchase.getPurchaseToken();
        synchronized (consuming) {
            if (!consuming.add(token)) {
                return;
            }
        }

        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(token)
                .build();
        billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> MAIN.post(() -> {
            synchronized (consuming) {
                consuming.remove(token);
            }
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                toast("Could not finish the purchase");
                return;
            }

            List<String> ids = purchase.getProducts();
            if (ids == null || ids.isEmpty()) {
                return;
            }
            String orderId = purchase.getOrderId();
            String transactionId = orderId == null || orderId.isEmpty() ? token : orderId;
            String productId = ids.get(0);
            int coins = CoinWallet.coinsFor(productId);
            if (coins > 0) {
                CoinWallet.add(appContext, coins);
            }
            Session.recordPurchase(appContext, productId, transactionId, token);
            toast(coins > 0 ? "+" + coins + " coins" : "Pack purchased");
        }));
    }

    private void notifyProductsChanged() {
        MAIN.post(() -> {
            for (Listener listener : listeners) {
                listener.onProductsChanged();
            }
        });
    }

    private void toast(String message) {
        MAIN.post(() -> Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show());
    }
}
