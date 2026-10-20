package com.sanangames.tapomaly;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    ImageView imgSplashLogo;
    TextView txtSplashTitle, txtSplashTagline, txtLoading;

    MediaPlayer mediaPlayer;

    private static final int SPLASH_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        imgSplashLogo = findViewById(R.id.imgSplashLogo);
        txtSplashTitle = findViewById(R.id.txtSplashTitle);
        txtSplashTagline = findViewById(R.id.txtSplashTagline);
        txtLoading = findViewById(R.id.txtLoading);

        playSplashSound();
        startSplashAnimations();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            stopSplashSound();

            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }

    private void playSplashSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.splash_sound);

            if (mediaPlayer != null) {
                mediaPlayer.setVolume(0.8f, 0.8f);
                mediaPlayer.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopSplashSound() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void startSplashAnimations() {
        ScaleAnimation logoScale = new ScaleAnimation(
                0.65f,
                1.0f,
                0.65f,
                1.0f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );

        logoScale.setDuration(1200);
        logoScale.setFillAfter(true);
        imgSplashLogo.startAnimation(logoScale);

        AlphaAnimation titleFade = new AlphaAnimation(0.0f, 1.0f);
        titleFade.setDuration(1400);
        titleFade.setStartOffset(500);
        titleFade.setFillAfter(true);

        txtSplashTitle.startAnimation(titleFade);
        txtSplashTagline.startAnimation(titleFade);

        AlphaAnimation loadingFade = new AlphaAnimation(0.2f, 1.0f);
        loadingFade.setDuration(700);
        loadingFade.setRepeatMode(Animation.REVERSE);
        loadingFade.setRepeatCount(Animation.INFINITE);
        txtLoading.startAnimation(loadingFade);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSplashSound();
    }
}