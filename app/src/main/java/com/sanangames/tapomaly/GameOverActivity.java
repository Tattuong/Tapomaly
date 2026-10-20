package com.sanangames.tapomaly;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameOverActivity extends AppCompatActivity {

    TextView txtGameOverLevel, txtGameOverScore;
    ImageButton btnTryAgain, btnGameOverMenu;

    SoundManager soundManager;

    int level;
    int score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        soundManager = new SoundManager(this);

        txtGameOverLevel = findViewById(R.id.txtGameOverLevel);
        txtGameOverScore = findViewById(R.id.txtGameOverScore);

        btnTryAgain = findViewById(R.id.btnTryAgain);
        btnGameOverMenu = findViewById(R.id.btnGameOverMenu);

        level = getIntent().getIntExtra("level", 1);
        score = getIntent().getIntExtra("score", 0);

        txtGameOverLevel.setText("Level " + level);
        txtGameOverScore.setText("Score: " + score);

        btnTryAgain.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(GameOverActivity.this, GameActivity.class);
            intent.putExtra("level", level);
            startActivity(intent);
            finish();
        });

        btnGameOverMenu.setOnClickListener(v -> {
            if (soundManager != null) {
                soundManager.playClick();
            }

            Intent intent = new Intent(GameOverActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (soundManager != null) {
            soundManager.release();
        }
    }
}