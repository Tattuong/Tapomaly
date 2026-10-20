package com.sanangames.tapomaly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    TextView txtResultScore, txtStars;
    ImageButton btnNextLevel, btnReplay, btnMenu;

    SoundManager soundManager;

    int level;
    int score;

    SharedPreferences sharedPreferences;

    public static final String PREF_NAME = "TapomalyPrefs";
    public static final String UNLOCKED_LEVEL_KEY = "unlockedLevel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        soundManager = new SoundManager(this);
        soundManager.playLevelComplete();

        txtResultScore = findViewById(R.id.txtResultScore);
        txtStars = findViewById(R.id.txtStars);

        btnNextLevel = findViewById(R.id.btnNextLevel);
        btnReplay = findViewById(R.id.btnReplay);
        btnMenu = findViewById(R.id.btnMenu);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        level = getIntent().getIntExtra("level", 1);
        score = getIntent().getIntExtra("score", 0);

        unlockNextLevel();

        txtResultScore.setText("Score: " + score);
        Session.rememberScore(this, score);
        if (savedInstanceState == null) {
            ProgressSync.save(this, false);
        }

        setStars();

        btnNextLevel.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(ResultActivity.this, GameActivity.class);
            intent.putExtra("level", level + 1);
            startActivity(intent);
            finish();
        });

        btnReplay.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(ResultActivity.this, GameActivity.class);
            intent.putExtra("level", level);
            startActivity(intent);
            finish();
        });

        btnMenu.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void unlockNextLevel() {
        int currentUnlockedLevel = sharedPreferences.getInt(UNLOCKED_LEVEL_KEY, 1);

        if (level >= currentUnlockedLevel) {
            sharedPreferences.edit()
                    .putInt(UNLOCKED_LEVEL_KEY, level + 1)
                    .apply();
        }
    }

    private void setStars() {
        if (score >= 100) {
            txtStars.setText("⭐⭐⭐");
        } else if (score >= 50) {
            txtStars.setText("⭐⭐");
        } else {
            txtStars.setText("⭐");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (soundManager != null) {
            soundManager.release();
        }
    }
}