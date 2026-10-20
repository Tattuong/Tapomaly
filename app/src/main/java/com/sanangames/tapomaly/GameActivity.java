package com.sanangames.tapomaly;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class GameActivity extends AppCompatActivity {

    TextView txtLevel, txtTimer, txtScore, txtInstruction, btnSave, btnOpenShop;
    GridLayout gameGrid;
    ImageButton btnHome;
    AlertDialog shopDialog;
    AlertDialog accountDialog;

    SoundManager soundManager;

    int level = 1;
    int score = 0;
    int timeLeft = 30;
    int totalTime = 30;
    int totalObjects = 16;
    int columns = 4;
    int wrongPenaltyTime = 2;

    int oddPosition;
    CountDownTimer countDownTimer;

    int normalImage = R.drawable.apple;
    int oddImage = R.drawable.apple_odd;

    String themeName = "Fruits";

    boolean isHomeDialogOpen = false;
    boolean isShopOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        soundManager = new SoundManager(this);

        txtLevel = findViewById(R.id.txtLevel);
        txtTimer = findViewById(R.id.txtTimer);
        txtScore = findViewById(R.id.txtScore);
        txtInstruction = findViewById(R.id.txtInstruction);
        btnSave = findViewById(R.id.btnSave);
        btnOpenShop = findViewById(R.id.btnOpenShop);
        gameGrid = findViewById(R.id.gameGrid);
        btnHome = findViewById(R.id.btnHome);


        btnHome.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }


            showHomeConfirmDialog();
        });

        btnSave.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }
            Session.rememberScore(this, score);
            if (Session.isSignedIn(this)) {
                ProgressSync.save(this, true);
            } else {
                openAccount();
            }
        });

        btnOpenShop.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }
            openShop();
        });
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        level = getIntent().getIntExtra("level", 1);

        setupLevelData();
        if (getIntent().getBooleanExtra("continueRun", false)) {
            score = getIntent().getIntExtra("score", 0);
            totalTime += 8;
        }
        setupLevel();
        startTimer();
    }

    private void openAccount() {
        if (isShopOpen || isHomeDialogOpen) {
            return;
        }
        isShopOpen = true;
        if (countDownTimer != null) {
            countDownTimer.cancel();
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
                isShopOpen = false;
                if (!isFinishing() && !isDestroyed() && !isHomeDialogOpen) {
                    resumeTimer();
                }
            }
        });
        if (accountDialog == null) {
            isShopOpen = false;
            resumeTimer();
        }
    }

    private void openShop() {
        if (isShopOpen || isHomeDialogOpen) {
            return;
        }

        isShopOpen = true;
        if (countDownTimer != null) {
            countDownTimer.cancel();
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
                isShopOpen = false;
                if (!isFinishing() && !isDestroyed() && !isHomeDialogOpen) {
                    resumeTimer();
                }
            }
        });

        if (shopDialog == null) {
            isShopOpen = false;
            resumeTimer();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupLevelData() {
        switch (level) {
            case 1:
                themeName = "Fruits";
                normalImage = R.drawable.apple;
                oddImage = R.drawable.apple_odd;
                totalObjects = 12;
                columns = 3;
                totalTime = 30;
                wrongPenaltyTime = 2;
                break;

            case 2:
                themeName = "Faces";
                normalImage = R.drawable.face;
                oddImage = R.drawable.face_odd;
                totalObjects = 16;
                columns = 4;
                totalTime = 28;
                wrongPenaltyTime = 2;
                break;

            case 3:
                themeName = "Vehicles";
                normalImage = R.drawable.car;
                oddImage = R.drawable.car_odd;
                totalObjects = 20;
                columns = 4;
                totalTime = 26;
                wrongPenaltyTime = 3;
                break;

            case 4:
                themeName = "Animals";
                normalImage = R.drawable.dog;
                oddImage = R.drawable.cat;
                totalObjects = 20;
                columns = 4;
                totalTime = 24;
                wrongPenaltyTime = 3;
                break;

            case 5:
                themeName = "Space";
                normalImage = R.drawable.star;
                oddImage = R.drawable.star_odd;
                totalObjects = 25;
                columns = 5;
                totalTime = 22;
                wrongPenaltyTime = 3;
                break;

            case 6:
                themeName = "Food";
                normalImage = R.drawable.burger;
                oddImage = R.drawable.pizza;
                totalObjects = 25;
                columns = 5;
                totalTime = 20;
                wrongPenaltyTime = 4;
                break;

            case 7:
                themeName = "Colors";
                normalImage = R.drawable.blue_square;
                oddImage = R.drawable.purple_square;
                totalObjects = 30;
                columns = 5;
                totalTime = 19;
                wrongPenaltyTime = 4;
                break;

            case 8:
                themeName = "Sports";
                normalImage = R.drawable.football;
                oddImage = R.drawable.basketball;
                totalObjects = 30;
                columns = 5;
                totalTime = 18;
                wrongPenaltyTime = 4;
                break;

            case 9:
                themeName = "Gems";
                normalImage = R.drawable.diamond;
                oddImage = R.drawable.diamond_odd;
                totalObjects = 35;
                columns = 5;
                totalTime = 17;
                wrongPenaltyTime = 5;
                break;

            case 10:
                themeName = "Royal";
                normalImage = R.drawable.crown;
                oddImage = R.drawable.hat;
                totalObjects = 36;
                columns = 6;
                totalTime = 16;
                wrongPenaltyTime = 5;
                break;

            default:
                themeName = "Challenge";
                normalImage = R.drawable.blue_square;
                oddImage = R.drawable.purple_square;
                totalObjects = 36;
                columns = 6;
                totalTime = 15;
                wrongPenaltyTime = 5;
                break;
        }
    }

    private void setupLevel() {
        txtLevel.setText("Level " + level);
        txtScore.setText("Score: " + score);
        txtInstruction.setText(themeName + " Theme: Find the odd object");

        gameGrid.removeAllViews();
        gameGrid.setColumnCount(columns);

        oddPosition = new Random().nextInt(totalObjects);

        for (int i = 0; i < totalObjects; i++) {
            ImageView item = new ImageView(this);

            if (i == oddPosition) {
                item.setImageResource(oddImage);
            } else {
                item.setImageResource(normalImage);
            }

            item.setBackgroundResource(R.drawable.bg_game_item);
            item.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            item.setScaleType(ImageView.ScaleType.FIT_CENTER);
            item.setAdjustViewBounds(true);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            int screenWidth = getResources().getDisplayMetrics().widthPixels;

// Main screen side padding from activity_game.xml
            int screenSidePadding = dpToPx(32);

// Margin on each item left + right
            int itemMargin = dpToPx(4);

// Total margins for all columns
            int totalMargins = columns * itemMargin * 2;

// Available width for grid items
            int availableWidth = screenWidth - screenSidePadding - totalMargins;

// Dynamic item size
            int itemSize = availableWidth / columns;

// Maximum size control, so easy levels do not become too huge
            if (columns == 3 && itemSize > dpToPx(88)) {
                itemSize = dpToPx(88);
            } else if (columns == 4 && itemSize > dpToPx(76)) {
                itemSize = dpToPx(76);
            } else if (columns == 5 && itemSize > dpToPx(62)) {
                itemSize = dpToPx(62);
            } else if (columns == 6 && itemSize > dpToPx(52)) {
                itemSize = dpToPx(52);
            }

            params.width = itemSize;
            params.height = itemSize;
            params.setMargins(itemMargin, itemMargin, itemMargin, itemMargin);

            item.setLayoutParams(params);

            int position = i;

            item.setOnClickListener(v -> {
                if (position == oddPosition) {
                    correctTap();
                } else {
                    wrongTap();
                }
            });

            gameGrid.addView(item);
        }
    }

    private void showHomeConfirmDialog() {
        if (isHomeDialogOpen) {
            return;
        }

        isHomeDialogOpen = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.create();

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setGravity(Gravity.CENTER);
        dialogLayout.setPadding(dpToPx(26), dpToPx(24), dpToPx(26), dpToPx(22));
        dialogLayout.setBackground(createDialogBackground());

        TextView title = new TextView(this);
        title.setText("Go Back Home?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText("Are you sure you want to leave this level?");
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
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        btnNo.setGravity(Gravity.CENTER);
        btnNo.setBackground(createSecondaryButtonBackground());
        btnNo.setPadding(dpToPx(30), dpToPx(12), dpToPx(30), dpToPx(12));

        TextView btnYes = new TextView(this);
        btnYes.setText("Yes");
        btnYes.setTextColor(Color.WHITE);
        btnYes.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
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

            isHomeDialogOpen = false;
            dialog.dismiss();
            resumeTimer();
        });

        btnYes.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            isHomeDialogOpen = false;
            dialog.dismiss();
            goToHome();
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

    private void goToHome() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        Intent intent = new Intent(GameActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void correctTap() {
        soundManager.playCorrect();

        int timeBonus = timeLeft * 2;
        int levelBonus = level * 20;

        score += 100 + timeBonus + levelBonus;
        txtScore.setText("Score: " + score);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        Intent intent = new Intent(GameActivity.this, ResultActivity.class);
        intent.putExtra("level", level);
        intent.putExtra("score", score);
        startActivity(intent);
        finish();
    }

    private void wrongTap() {
        soundManager.playWrong();

        score -= 10;

        if (score < 0) {
            score = 0;
        }

        timeLeft -= wrongPenaltyTime;

        if (timeLeft < 0) {
            timeLeft = 0;
        }

        txtScore.setText("Score: " + score);
        txtTimer.setText("Time: " + timeLeft);

        Toast.makeText(
                GameActivity.this,
                "Wrong! -" + wrongPenaltyTime + " seconds",
                Toast.LENGTH_SHORT
        ).show();

        if (timeLeft <= 0) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            openGameOver();
        }
    }

    private void startTimer() {
        timeLeft = totalTime;
        txtTimer.setText("Time: " + timeLeft);
        runTimer(timeLeft);
    }

    private void resumeTimer() {
        if (timeLeft <= 0) {
            openGameOver();
            return;
        }

        txtTimer.setText("Time: " + timeLeft);
        runTimer(timeLeft);
    }

    private void runTimer(int seconds) {
        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = (int) millisUntilFinished / 1000;
                txtTimer.setText("Time: " + timeLeft);
            }

            @Override
            public void onFinish() {
                timeLeft = 0;
                txtTimer.setText("Time: 0");
                openGameOver();
            }
        };

        countDownTimer.start();
    }

    private void openGameOver() {
        soundManager.playGameOver();

        Intent intent = new Intent(GameActivity.this, GameOverActivity.class);
        intent.putExtra("level", level);
        intent.putExtra("score", score);
        startActivity(intent);
        finish();
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

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (soundManager != null) {
            soundManager.release();
        }
    }
}