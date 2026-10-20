package com.sanangames.tapomaly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class LevelSelectActivity extends AppCompatActivity {

    Button btnLevel1, btnLevel2, btnLevel3, btnLevel4, btnLevel5;
    Button btnLevel6, btnLevel7, btnLevel8, btnLevel9, btnLevel10;
    ImageButton btnBackMenu;

    SharedPreferences sharedPreferences;
    SoundManager soundManager;

    public static final String PREF_NAME = "TapomalyPrefs";
    public static final String UNLOCKED_LEVEL_KEY = "unlockedLevel";

    int unlockedLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        soundManager = new SoundManager(this);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        unlockedLevel = sharedPreferences.getInt(UNLOCKED_LEVEL_KEY, 1);

        btnLevel1 = findViewById(R.id.btnLevel1);
        btnLevel2 = findViewById(R.id.btnLevel2);
        btnLevel3 = findViewById(R.id.btnLevel3);
        btnLevel4 = findViewById(R.id.btnLevel4);
        btnLevel5 = findViewById(R.id.btnLevel5);
        btnLevel6 = findViewById(R.id.btnLevel6);
        btnLevel7 = findViewById(R.id.btnLevel7);
        btnLevel8 = findViewById(R.id.btnLevel8);
        btnLevel9 = findViewById(R.id.btnLevel9);
        btnLevel10 = findViewById(R.id.btnLevel10);
        btnBackMenu = findViewById(R.id.btnBackMenu);

        setupLevelButton(btnLevel1, 1);
        setupLevelButton(btnLevel2, 2);
        setupLevelButton(btnLevel3, 3);
        setupLevelButton(btnLevel4, 4);
        setupLevelButton(btnLevel5, 5);
        setupLevelButton(btnLevel6, 6);
        setupLevelButton(btnLevel7, 7);
        setupLevelButton(btnLevel8, 8);
        setupLevelButton(btnLevel9, 9);
        setupLevelButton(btnLevel10, 10);

        btnBackMenu.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(LevelSelectActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupLevelButton(Button button, int levelNumber) {
        if (levelNumber <= unlockedLevel) {
            button.setText("Level " + levelNumber);
            button.setEnabled(true);
            button.setAlpha(1.0f);
            button.setTextColor(getResources().getColor(android.R.color.white));

            button.setOnClickListener(v -> {
                if (soundManager != null) {
                    soundManager.playClick();
                }

                openLevel(levelNumber);
            });

        } else {
            button.setText("🔒 Level " + levelNumber);
            button.setEnabled(false);
            button.setAlpha(0.45f);
            button.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void openLevel(int levelNumber) {
        Intent intent = new Intent(LevelSelectActivity.this, GameActivity.class);
        intent.putExtra("level", levelNumber);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (soundManager != null) {
            soundManager.release();
        }
    }
}