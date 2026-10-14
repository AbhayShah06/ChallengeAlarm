package com.abhayshah.challengealarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class AlarmStore {
    private static final String PREFS = "alarms";
    private static final String DATA = "items";
    private AlarmStore() {}

    static List<AlarmItem> all(Context context) {
        ArrayList<AlarmItem> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(context).getString(DATA, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new AlarmItem(o.getInt("id"), o.getInt("hour"), o.getInt("minute"), o.optInt("days"), o.optInt("difficulty", 2), o.optBoolean("enabled", true)));
            }
        } catch (Exception ignored) {}
        Collections.sort(result, Comparator.comparing((AlarmItem a) -> !a.enabled).thenComparingInt(a -> a.hour * 60 + a.minute));
        return result;
    }

    static void save(Context context, List<AlarmItem> alarms) {
        JSONArray array = new JSONArray();
        try { for (AlarmItem a : alarms) { JSONObject o = new JSONObject(); o.put("id", a.id); o.put("hour", a.hour); o.put("minute", a.minute); o.put("days", a.days); o.put("difficulty", a.difficulty); o.put("enabled", a.enabled); array.put(o); } }
        catch (Exception ignored) {}
        prefs(context).edit().putString(DATA, array.toString()).apply();
    }

    static void scheduleAll(Context context) {
        for (AlarmItem a : all(context)) schedule(context, a);
    }

    static void schedule(Context context, AlarmItem alarm) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        PendingIntent pending = pending(context, alarm.id);
        manager.cancel(pending);
        if (!alarm.enabled || !canSchedule(context)) return;
        long at = nextTime(alarm, System.currentTimeMillis());
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(at, pending), pending);
    }

    static void cancel(Context context, int id) {
        context.getSystemService(AlarmManager.class).cancel(pending(context, id));
    }

    static boolean canSchedule(Context context) {
        return android.os.Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    static void onTriggered(Context context, int id) {
        List<AlarmItem> alarms = all(context);
        for (AlarmItem a : alarms) if (a.id == id) {
            if (a.days == 0) { a.enabled = false; save(context, alarms); }
            else schedule(context, a);
            return;
        }
    }

    static AlarmItem find(Context context, int id) {
        for (AlarmItem alarm : all(context)) if (alarm.id == id) return alarm;
        return null;
    }

    static long nextTime(AlarmItem alarm, long now) {
        Calendar candidate = Calendar.getInstance();
        candidate.setTimeInMillis(now);
        candidate.set(Calendar.SECOND, 0); candidate.set(Calendar.MILLISECOND, 0);
        candidate.set(Calendar.HOUR_OF_DAY, alarm.hour); candidate.set(Calendar.MINUTE, alarm.minute);
        for (int offset = 0; offset <= 7; offset++) {
            candidate.setTimeInMillis(now);
            candidate.set(Calendar.SECOND, 0); candidate.set(Calendar.MILLISECOND, 0);
            candidate.add(Calendar.DAY_OF_YEAR, offset);
            candidate.set(Calendar.HOUR_OF_DAY, alarm.hour); candidate.set(Calendar.MINUTE, alarm.minute);
            int dayBit = 1 << (candidate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY);
            if (candidate.getTimeInMillis() <= now || (alarm.days != 0 && (alarm.days & dayBit) == 0)) continue;
            return candidate.getTimeInMillis();
        }
        return now + 24L * 60 * 60 * 1000;
    }

    private static PendingIntent pending(Context context, int id) {
        Intent intent = new Intent(context, AlarmReceiver.class).setAction("com.abhayshah.challengealarm.ALARM." + id).putExtra("alarmId", id);
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
}
