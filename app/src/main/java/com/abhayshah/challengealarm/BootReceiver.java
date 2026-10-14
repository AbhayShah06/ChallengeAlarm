package com.abhayshah.challengealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        // Phase 1 persists alarms in Room and reschedules every enabled alarm here.
    }
}

