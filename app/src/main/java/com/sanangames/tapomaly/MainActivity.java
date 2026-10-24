package com.sanangames.tapomaly;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    ImageButton btnPlay, btnLevels, btnSettings, btnExit;
    TextView btnAccount, btnShop, txtMenuCoins;

    SoundManager soundManager;
    AlertDialog shopDialog;
    AlertDialog accountDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        soundManager = new SoundManager(this);

        btnPlay = findViewById(R.id.btnPlay);
        btnLevels = findViewById(R.id.btnLevels);
        btnSettings = findViewById(R.id.btnSettings);
        btnExit = findViewById(R.id.btnExit);
        btnAccount = findViewById(R.id.btnAccount);
        btnShop = findViewById(R.id.btnShop);
        txtMenuCoins = findViewById(R.id.txtMenuCoins);
        refreshAccount();

        btnPlay.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("level", 1);
            startActivity(intent);
        });

        btnLevels.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(MainActivity.this, LevelSelectActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            showExitConfirmDialog();
        });

        btnShop.setOnClickListener(v -> openShop());
        btnAccount.setOnClickListener(v -> openAccount());
        keepMenuButtonsBelowSystemBars();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccount();
    }

    private void refreshAccount() {
        if (btnAccount != null) {
            btnAccount.setText(Session.label(this));
        }
        if (txtMenuCoins != null) {
            txtMenuCoins.setText(CoinWallet.label(this));
        }
    }

    private void openAccount() {
        if (soundManager != null) {
            soundManager.playClick();
        }
        if (accountDialog != null && accountDialog.isShowing()) {
            return;
        }
        accountDialog = AccountSheet.show(this, new AccountSheet.Host() {
            @Override
            public void playClick() {
                if (soundManager != null) {
                    soundManager.playClick();
                }
            }

            @Override
            public void onClosed() {
                refreshAccount();
            }
        });
    }

    private void keepMenuButtonsBelowSystemBars() {
        View topBar = findViewById(R.id.menuTopBar);
        final int padStart = topBar.getPaddingStart();
        final int padTop = topBar.getPaddingTop();
        final int padEnd = topBar.getPaddingEnd();
        final int padBottom = topBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (view, windowInsets) -> {
            placeTopBarClearOfSystem(view, windowInsets, padStart, padTop, padEnd, padBottom);
            return windowInsets;
        });
        topBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                keepShopTappable()
        );
        ViewCompat.requestApplyInsets(topBar);
    }

    private void placeTopBarClearOfSystem(
            View view,
            WindowInsetsCompat windowInsets,
            int padStart,
            int padTop,
            int padEnd,
            int padBottom
    ) {
        Insets bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
        );
        int top = Math.max(padTop, bars.top + dpToPx(6));
        view.setPadding(padStart + bars.left, top, padEnd + bars.right, padBottom);
    }

    private void keepShopTappable() {
        View topBar = findViewById(R.id.menuTopBar);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || topBar == null || btnShop == null || btnShop.getWidth() == 0) {
            return;
        }
        Rect hit = new Rect();
        btnShop.getHitRect(hit);
        Rect limit = new Rect(0, 0, topBar.getWidth(), topBar.getHeight());
        if (hit.intersect(limit)) {
            topBar.setSystemGestureExclusionRects(Collections.singletonList(new Rect(hit)));
        }
    }

    private void openShop() {
        if (soundManager != null) {
            soundManager.playClick();
        }
        if (shopDialog != null && shopDialog.isShowing()) {
            return;
        }

        shopDialog = ShopPopup.show(this, new ShopPopup.Host() {
            @Override
            public void playClick() {
                if (soundManager != null) {
                    soundManager.playClick();
                }
            }

            @Override
            public void onShopClosed() {
                refreshAccount();
            }
        });
    }

    private void showExitConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.create();

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setGravity(Gravity.CENTER);
        dialogLayout.setPadding(dpToPx(26), dpToPx(24), dpToPx(26), dpToPx(22));
        dialogLayout.setBackground(createDialogBackground());

        TextView title = new TextView(this);
        title.setText("Exit Game?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText("Are you sure you want to exit Tapomaly?");
        message.setTextColor(Color.parseColor("#D8E2F3"));
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dpToPx(12), 0, dpToPx(24));

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);

        TextView btnNo = new TextView(this);
        btnNo.setText("No");
        btnNo.setTextColor(Color.WHITE);
        btnNo.setTextSize(17);
        btnNo.setTypeface(null, Typeface.BOLD);
        btnNo.setGravity(Gravity.CENTER);
        btnNo.setBackground(createSecondaryButtonBackground());
        btnNo.setPadding(dpToPx(30), dpToPx(12), dpToPx(30), dpToPx(12));

        TextView btnYes = new TextView(this);
        btnYes.setText("Yes");
        btnYes.setTextColor(Color.WHITE);
        btnYes.setTextSize(17);
        btnYes.setTypeface(null, Typeface.BOLD);
        btnYes.setGravity(Gravity.CENTER);
        btnYes.setBackground(createPrimaryButtonBackground());
        btnYes.setPadding(dpToPx(30), dpToPx(12), dpToPx(30), dpToPx(12));

        LinearLayout.LayoutParams noParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        noParams.setMargins(0, 0, dpToPx(8), 0);

        LinearLayout.LayoutParams yesParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        yesParams.setMargins(dpToPx(8), 0, 0, 0);

        buttonLayout.addView(btnNo, noParams);
        buttonLayout.addView(btnYes, yesParams);

        dialogLayout.addView(title);
        dialogLayout.addView(message);
        dialogLayout.addView(buttonLayout);

        dialog.setView(dialogLayout);
        dialog.setCancelable(false);

        btnNo.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            dialog.dismiss();
        });

        btnYes.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            dialog.dismiss();
            finishAffinity();
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        dialog.show();
    }

    private GradientDrawable createDialogBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.parseColor("#20113F"),
                        Color.parseColor("#121A33"),
                        Color.parseColor("#061B3A")
                }
        );

        drawable.setCornerRadius(dpToPx(26));
        drawable.setStroke(dpToPx(2), Color.parseColor("#8B5CF6"));

        return drawable;
    }

    private GradientDrawable createPrimaryButtonBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.parseColor("#06D6A0"),
                        Color.parseColor("#00B4D8")
                }
        );

        drawable.setCornerRadius(dpToPx(18));
        return drawable;
    }

    private GradientDrawable createSecondaryButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#26324A"));
        drawable.setCornerRadius(dpToPx(18));
        drawable.setStroke(dpToPx(1), Color.parseColor("#6D7EA8"));
        return drawable;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (shopDialog != null && shopDialog.isShowing()) {
            shopDialog.dismiss();
        }
        if (accountDialog != null && accountDialog.isShowing()) {
            accountDialog.dismiss();
        }
        super.onDestroy();

        if (soundManager != null) {
            soundManager.release();
        }
    }
}