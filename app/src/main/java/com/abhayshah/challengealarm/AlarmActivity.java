package com.abhayshah.challengealarm;

import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.EditText;
import android.widget.TextView;

public class AlarmActivity extends Activity {
    private Ringtone ringtone;
    private Vibrator vibrator;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); setShowWhenLocked(true); setTurnScreenOn(true); setContentView(R.layout.activity_alarm);
        ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        if (ringtone != null) ringtone.play();
        vibrator = getSystemService(Vibrator.class);
        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 700, 300}, 0));
        findViewById(R.id.dismiss).setOnClickListener(v -> checkAnswer());
    }
    private void checkAnswer() {
        String answer = ((EditText)findViewById(R.id.answer)).getText().toString();
        if ("43".equals(answer)) { stopAlarm(); finishAndRemoveTask(); }
        else ((TextView)findViewById(R.id.error)).setText("Not quite. Try again.");
    }
    private void stopAlarm() { if (ringtone != null) ringtone.stop(); if (vibrator != null) vibrator.cancel(); }
    @Override public void onDestroy() { stopAlarm(); super.onDestroy(); }
}

