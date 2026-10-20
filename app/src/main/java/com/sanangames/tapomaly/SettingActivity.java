package com.sanangames.tapomaly;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity {

    Switch switchMusic, switchSound, switchVibration;
    ImageButton btnResetProgress, btnSettingsBack;

    SharedPreferences sharedPreferences;
    SoundManager soundManager;

    public static final String PREF_NAME = "TapomalyPrefs";
    public static final String MUSIC_KEY = "music";
    public static final String SOUND_KEY = "sound";
    public static final String VIBRATION_KEY = "vibration";
    public static final String UNLOCKED_LEVEL_KEY = "unlockedLevel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        soundManager = new SoundManager(this);

        switchMusic = findViewById(R.id.switchMusic);
        switchSound = findViewById(R.id.switchSound);
        switchVibration = findViewById(R.id.switchVibration);

        btnResetProgress = findViewById(R.id.btnResetProgress);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        loadSettings();

        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(MUSIC_KEY, isChecked).apply();
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(SOUND_KEY, isChecked).apply();
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(VIBRATION_KEY, isChecked).apply();
        });

        btnResetProgress.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            showPremiumConfirmDialog(
                    "Reset Progress?",
                    "Are you sure you want to reset your game progress?",
                    "No",
                    "Yes",
                    new Runnable() {
                        @Override
                        public void run() {
                            resetProgress();
                        }
                    }
            );
        });

        btnSettingsBack.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            showPremiumConfirmDialog(
                    "Back to Menu?",
                    "Are you sure you want to go back to menu?",
                    "No",
                    "Yes",
                    new Runnable() {
                        @Override
                        public void run() {
                            goBackToMenu();
                        }
                    }
            );
        });
    }

    private void loadSettings() {
        boolean music = sharedPreferences.getBoolean(MUSIC_KEY, true);
        boolean sound = sharedPreferences.getBoolean(SOUND_KEY, true);
        boolean vibration = sharedPreferences.getBoolean(VIBRATION_KEY, true);

        switchMusic.setChecked(music);
        switchSound.setChecked(sound);
        switchVibration.setChecked(vibration);
    }

    private void resetProgress() {
        sharedPreferences.edit()
                .putInt(UNLOCKED_LEVEL_KEY, 1)
                .apply();

        Toast.makeText(this, "Progress reset successfully", Toast.LENGTH_SHORT).show();
    }

    private void goBackToMenu() {
        Intent intent = new Intent(SettingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showPremiumConfirmDialog(
            String titleText,
            String messageText,
            String negativeText,
            String positiveText,
            Runnable positiveAction
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.create();

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setGravity(Gravity.CENTER);
        dialogLayout.setPadding(dpToPx(26), dpToPx(24), dpToPx(26), dpToPx(22));
        dialogLayout.setBackground(createDialogBackground());

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText(messageText);
        message.setTextColor(Color.parseColor("#D8E2F3"));
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dpToPx(12), 0, dpToPx(24));

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);

        TextView btnNo = new TextView(this);
        btnNo.setText(negativeText);
        btnNo.setTextColor(Color.WHITE);
        btnNo.setTextSize(17);
        btnNo.setTypeface(null, Typeface.BOLD);
        btnNo.setGravity(Gravity.CENTER);
        btnNo.setBackground(createSecondaryButtonBackground());
        btnNo.setPadding(dpToPx(30), dpToPx(12), dpToPx(30), dpToPx(12));

        TextView btnYes = new TextView(this);
        btnYes.setText(positiveText);
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

            if (positiveAction != null) {
                positiveAction.run();
            }
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
        super.onDestroy();

        if (soundManager != null) {
            soundManager.release();
        }
    }
}