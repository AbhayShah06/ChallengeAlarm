package com.abhayshah.challengealarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

public class AlarmService extends Service {
    static final String ACTION_START = "com.abhayshah.challengealarm.START_ALARM";
    static final String ACTION_STOP = "com.abhayshah.challengealarm.STOP_ALARM";
    private static final String CHANNEL = "ringing_alarm";
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private int previousAlarmVolume = -1;
    private PowerManager.WakeLock wakeLock;
    private Vibrator vibrator;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        createChannel(); int id = intent.getIntExtra("alarmId", 0);
        startForeground(1001, notification(id)); acquireWakeLock(); startSound(); startVibration();
        return START_NOT_STICKY;
    }
    private Notification notification(int id) {
        Intent screen = new Intent(this, AlarmActivity.class).putExtra("alarmId", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(this, id, screen, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Alarm ringing").setContentText("Solve the problem to dismiss")
                .setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreen, true).setContentIntent(fullScreen).setOngoing(true).build();
    }
    private void createChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Alarms", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alarm alerts"); channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); channel.setSound(null, null); manager.createNotificationChannel(channel);
        }
    }
    private void startSound() {
        audioManager = getSystemService(AudioManager.class);
        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(change -> {
                        if (player == null) return;
                        if (change == AudioManager.AUDIOFOCUS_GAIN) player.setVolume(1f, 1f);
                        else if (change < 0) player.setVolume(.35f, .35f);
                    }).build();
            audioManager.requestAudioFocus(focusRequest);
        } else audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        int maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0);
        Uri sound = Settings.System.DEFAULT_ALARM_ALERT_URI;
        if (sound == null) sound = Settings.System.DEFAULT_NOTIFICATION_URI;
        try {
            player = new MediaPlayer(); player.setAudioAttributes(attributes); player.setDataSource(this, sound); player.setLooping(true);
            player.setOnPreparedListener(MediaPlayer::start); player.setOnErrorListener((mp, what, extra) -> { stopSelf(); return true; }); player.prepareAsync();
        } catch (Exception e) { stopSelf(); }
    }
    private void acquireWakeLock() {
        wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":alarm");
        wakeLock.acquire(10 * 60 * 1000L);
    }
    private void startVibration() {
        vibrator = getSystemService(Vibrator.class);
        if (vibrator != null && vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 700, 300}, 0));
    }
    @Override public void onDestroy() {
        if (player != null) { if (player.isPlaying()) player.stop(); player.release(); player = null; }
        if (audioManager != null) {
            if (previousAlarmVolume >= 0) audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0);
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
            else audioManager.abandonAudioFocus(null);
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (vibrator != null) vibrator.cancel();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1001); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
