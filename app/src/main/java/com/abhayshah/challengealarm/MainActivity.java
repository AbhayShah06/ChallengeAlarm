package com.abhayshah.challengealarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_main);
        findViewById(R.id.schedule).setOnClickListener(v -> scheduleTest());
    }
    private void scheduleTest() {
        AlarmManager alarms = getSystemService(AlarmManager.class);
        if (!alarms.canScheduleExactAlarms()) {
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            ((TextView)findViewById(R.id.status)).setText("Grant exact-alarm access, then tap Schedule again.");
            return;
        }
        Intent intent = new Intent(this, AlarmReceiver.class).putExtra("alarmId", 1);
        PendingIntent pending = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long at = System.currentTimeMillis() + 60_000;
        alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(at, pending), pending);
        ((TextView)findViewById(R.id.status)).setText("Test alarm scheduled for one minute from now.");
    }
}

