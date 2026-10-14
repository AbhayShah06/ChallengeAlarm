package com.abhayshah.challengealarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout rows;
    private TextView status;
    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24), dp(24), dp(24), dp(16)); root.setBackgroundColor(0xff101214);
        TextView title = new TextView(this); title.setText("Challenge Alarm"); title.setTextSize(28); title.setTextColor(0xfff4f4f4); title.setTypeface(null, android.graphics.Typeface.BOLD); root.addView(title);
        status = new TextView(this); status.setTextSize(14); status.setPadding(0, dp(8), 0, dp(8)); root.addView(status);
        UpdateManager.attach(this, root);
        Button add = new Button(this); add.setText("+  Add alarm"); add.setOnClickListener(v -> chooseTime(null)); root.addView(add);
        ScrollView scroll = new ScrollView(this); rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); scroll.addView(rows); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
        refresh();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 41);
    }

    @Override protected void onResume() { super.onResume(); if (rows != null) { AlarmStore.scheduleAll(this); refresh(); } }

    private void refresh() {
        rows.removeAllViews();
        List<AlarmItem> alarms = AlarmStore.all(this);
        status.setText(alarms.isEmpty() ? "Set a time and solve a quick math problem to dismiss."
                : (!AlarmStore.canSchedule(this) ? "Allow exact alarm access in Settings to schedule alarms." : "Your alarms"));
        for (AlarmItem alarm : alarms) {
            LinearLayout card = new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(14), dp(9), dp(10), dp(9)); card.setBackgroundColor(0xff202528); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, dp(6), 0, dp(6)); rows.addView(card, cp);
            LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(0, dp(4), 0, dp(4)); card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            TextView time = new TextView(this); time.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)); time.setTextSize(32); time.setTextColor(alarm.enabled ? 0xfff4f4f4 : 0xff888888); info.addView(time);
            TextView days = new TextView(this); days.setText((alarm.days == 0 ? "One time" : daysText(alarm.days)) + "  ·  Math tier " + alarm.difficulty); days.setTextSize(13); days.setTextColor(0xffb8c0bc); info.addView(days);
            Button toggle = new Button(this); toggle.setText(alarm.enabled ? "On" : "Off"); toggle.setOnClickListener(v -> { alarm.enabled = !alarm.enabled; persist(alarm); }); card.addView(toggle);
            card.setOnClickListener(v -> chooseTime(alarm)); card.setOnLongClickListener(v -> { new android.app.AlertDialog.Builder(this).setMessage("Delete this alarm?").setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, w) -> { AlarmStore.cancel(this, alarm.id); List<AlarmItem> list = AlarmStore.all(this); list.removeIf(a -> a.id == alarm.id); AlarmStore.save(this, list); refresh(); }).show(); return true; });
        }
    }

    private void chooseTime(AlarmItem existing) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int hour = existing == null ? c.get(java.util.Calendar.HOUR_OF_DAY) : existing.hour;
        int minute;
        if (existing == null) {
            minute = (c.get(java.util.Calendar.MINUTE) + 4) / 5 * 5;
            if (minute == 60) { minute = 0; hour = (hour + 1) % 24; }
        } else minute = existing.minute;
        new TimePickerDialog(this, (picker, h, m) -> chooseDays(existing, h, m), hour, minute, android.text.format.DateFormat.is24HourFormat(this)).show();
    }

    private void chooseDays(AlarmItem old, int hour, int minute) {
        String[] choices = {"Never repeat", "Every day", "Weekdays", "Weekends", "Choose days…"};
        new android.app.AlertDialog.Builder(this).setTitle("Repeat").setItems(choices, (dialog, which) -> {
            if (which < 4) {
                int mask = which == 0 ? 0 : which == 1 ? 127 : which == 2 ? 62 : 65;
                chooseDifficulty(old, hour, minute, mask);
            } else chooseSpecificDays(old, hour, minute);
        }).show();
    }

    private void chooseSpecificDays(AlarmItem old, int hour, int minute) {
        boolean[] checked = new boolean[7]; if (old != null) for (int i = 0; i < 7; i++) checked[i] = (old.days & (1 << i)) != 0;
        new android.app.AlertDialog.Builder(this).setTitle("Repeat on").setMultiChoiceItems(DAYS, checked, (d, i, value) -> checked[i] = value)
            .setNegativeButton("Cancel", null).setPositiveButton("Next", (d, w) -> { int mask = 0; for (int i = 0; i < 7; i++) if (checked[i]) mask |= 1 << i; chooseDifficulty(old, hour, minute, mask); }).show();
    }

    private void chooseDifficulty(AlarmItem old, int hour, int minute, int days) {
        String[] tiers = {"Tier 1 · Super easy", "Tier 2 · Easy", "Tier 3 · Medium", "Tier 4 · Hard", "Tier 5 · Very hard"};
        int selected = old == null ? 1 : old.difficulty - 1;
        new android.app.AlertDialog.Builder(this).setTitle("Math difficulty")
            .setSingleChoiceItems(tiers, selected, null)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (dialog, which) -> {
                int picked = ((android.app.AlertDialog) dialog).getListView().getCheckedItemPosition();
                saveAlarm(old, hour, minute, days, picked + 1);
            }).show();
    }

    private void saveAlarm(AlarmItem old, int hour, int minute, int days, int difficulty) {
        List<AlarmItem> alarms = AlarmStore.all(this); int id = old == null ? nextId(alarms) : old.id;
        if (old != null) alarms.removeIf(a -> a.id == id);
        alarms.add(new AlarmItem(id, hour, minute, days, difficulty, true)); AlarmStore.save(this, alarms);
        if (!AlarmStore.canSchedule(this)) startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
        else AlarmStore.schedule(this, new AlarmItem(id, hour, minute, days, difficulty, true));
        refresh();
    }

    private void persist(AlarmItem changed) {
        List<AlarmItem> alarms = AlarmStore.all(this); alarms.removeIf(a -> a.id == changed.id); alarms.add(changed); AlarmStore.save(this, alarms);
        if (changed.enabled) { if (!AlarmStore.canSchedule(this)) startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)); else AlarmStore.schedule(this, changed); }
        else AlarmStore.cancel(this, changed.id);
        refresh();
    }

    private int nextId(List<AlarmItem> alarms) { int max = 0; for (AlarmItem a : alarms) max = Math.max(max, a.id); return max + 1; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    static String daysText(int mask) { StringBuilder b = new StringBuilder(); for (int i = 0; i < 7; i++) if ((mask & (1 << i)) != 0) { if (b.length() > 0) b.append(" · "); b.append(DAYS[i]); } return b.toString(); }
}
