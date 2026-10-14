package com.abhayshah.challengealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("alarmId", -1);
        if (id < 0) return;
        AlarmStore.onTriggered(context, id);
        Intent service = new Intent(context, AlarmService.class).setAction(AlarmService.ACTION_START).putExtra("alarmId", id);
        context.startForegroundService(service);
    }
}
