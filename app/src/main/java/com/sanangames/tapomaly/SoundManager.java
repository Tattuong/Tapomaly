package com.sanangames.tapomaly;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundManager {

    private SoundPool soundPool;

    private int clickSound;
    private int correctSound;
    private int wrongSound;
    private int gameOverSound;
    private int levelCompleteSound;

    private Context context;

    public static final String PREF_NAME = "TapomalyPrefs";
    public static final String SOUND_KEY = "sound";

    public SoundManager(Context context) {
        this.context = context;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        clickSound = soundPool.load(context, R.raw.click, 1);
        correctSound = soundPool.load(context, R.raw.correct, 1);
        wrongSound = soundPool.load(context, R.raw.wrong, 1);
        gameOverSound = soundPool.load(context, R.raw.game_over, 1);
        levelCompleteSound = soundPool.load(context, R.raw.level_complete, 1);
    }

    private boolean isSoundEnabled() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(SOUND_KEY, true);
    }

    private void playSound(int soundId) {
        if (isSoundEnabled()) {
            soundPool.play(soundId, 1, 1, 1, 0, 1);
        }
    }

    public void playClick() {
        playSound(clickSound);
    }

    public void playCorrect() {
        playSound(correctSound);
    }

    public void playWrong() {
        playSound(wrongSound);
    }

    public void playGameOver() {
        playSound(gameOverSound);
    }

    public void playLevelComplete() {
        playSound(levelCompleteSound);
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}