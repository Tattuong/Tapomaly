package com.sanangames.tapomaly;

import android.app.Application;

public class TapomalyApp extends Application {

    private static TapomalyApp instance;
    private BillingManager billingManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        IapConfig.fetch(this);
    }

    public static void startBilling() {
        if (instance == null || !IapConfig.isBillingAllowed()) {
            return;
        }
        if (instance.billingManager == null) {
            instance.billingManager = new BillingManager(instance);
        }
        instance.billingManager.start();
    }

    public static void stopBilling() {
        if (instance == null || instance.billingManager == null) {
            return;
        }
        instance.billingManager.end();
        instance.billingManager = null;
    }

    public static BillingManager billing() {
        if (instance == null || !IapConfig.isBillingAllowed()) {
            return null;
        }
        return instance.billingManager;
    }
}
