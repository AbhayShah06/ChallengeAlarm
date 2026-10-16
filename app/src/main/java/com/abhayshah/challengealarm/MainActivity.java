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
import android.widget.Switch;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout rows;
    private TextView status;
    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setNavigationBarColor(0xff0d0e10); getWindow().setStatusBarColor(0xff0d0e10);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(18), dp(20), dp(12)); root.setBackgroundColor(0xff0d0e10);
        TextView eyebrow = label("WAKE ON PURPOSE", 11, 0xffff8063, true); eyebrow.setLetterSpacing(.16f); root.addView(eyebrow);
        TextView title = label("Alarms", 38, 0xfff7f4ee, true); title.setPadding(0, dp(3), 0, 0); root.addView(title);
        status = label("", 15, 0xffaaa9ad, false); status.setPadding(0, dp(6), 0, dp(16)); root.addView(status);
        UpdateManager.attach(this, root);
        Button add = new Button(this); add.setText("Add alarm"); add.setTextSize(16); add.setTextColor(0xff111114); add.setBackgroundResource(R.drawable.bg_primary); add.setOnClickListener(v -> chooseTime(null)); LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(58)); ap.setMargins(0, 0, 0, dp(14)); root.addView(add, ap);
        ScrollView scroll = new ScrollView(this); rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); scroll.addView(rows); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
        refresh();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 41);
    }

    @Override protected void onResume() { super.onResume(); if (rows != null) { AlarmStore.scheduleAll(this); refresh(); } }

    private void refresh() {
        rows.removeAllViews();
        List<AlarmItem> alarms = AlarmStore.all(this);
        long now = System.currentTimeMillis(), next = Long.MAX_VALUE; int enabled = 0;
        for (AlarmItem alarm : alarms) if (alarm.enabled) { enabled++; next = Math.min(next, AlarmStore.nextTime(alarm, now)); }
        status.setText(!AlarmStore.canSchedule(this) ? "Exact-alarm access is off. Tap here to fix it."
                : enabled == 0 ? "No alarms armed. Your mornings are currently unsupervised."
                : enabled + (enabled == 1 ? " alarm armed" : " alarms armed") + "  ·  Next " + DateUtils.getRelativeTimeSpanString(next, now, DateUtils.MINUTE_IN_MILLIS));
        status.setOnClickListener(v -> { if (!AlarmStore.canSchedule(this)) startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)); });
        if (alarms.isEmpty()) {
            LinearLayout empty = new LinearLayout(this); empty.setOrientation(LinearLayout.VERTICAL); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(22), dp(48), dp(22), dp(48)); empty.setBackgroundResource(R.drawable.bg_card); rows.addView(empty);
            TextView emptyTitle = label("Nothing between you and oversleeping", 20, 0xfff7f4ee, true); emptyTitle.setGravity(Gravity.CENTER); empty.addView(emptyTitle);
            TextView emptyBody = label("Add one alarm. When it rings, solve the math and get up.", 14, 0xff96979d, false); emptyBody.setGravity(Gravity.CENTER); emptyBody.setPadding(0, dp(10), 0, 0); empty.addView(emptyBody); return;
        }
        for (AlarmItem alarm : alarms) {
            LinearLayout card = new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(18), dp(15), dp(12), dp(15)); card.setBackgroundResource(R.drawable.bg_card); card.setAlpha(alarm.enabled ? 1f : .58f); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, 0, 0, dp(10)); rows.addView(card, cp);
            LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(0, dp(4), 0, dp(4)); card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            TextView time = label(String.format(java.util.Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute), 38, alarm.enabled ? 0xfff7f4ee : 0xff8d8f96, true); info.addView(time);
            TextView days = label((alarm.days == 0 ? "Once" : daysText(alarm.days)) + "  ·  Math " + difficultyName(alarm.difficulty), 13, 0xff9a9ba1, false); info.addView(days);
            Switch toggle = new Switch(this); toggle.setChecked(alarm.enabled); toggle.setContentDescription((alarm.enabled ? "Disable" : "Enable") + " alarm at " + time.getText()); toggle.setOnCheckedChangeListener((button, checked) -> { if (checked == alarm.enabled) return; alarm.enabled = checked; persist(alarm); }); card.addView(toggle);
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
    private TextView label(String text, float size, int color, boolean bold) { TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); if (bold) view.setTypeface(Typeface.create("sans", Typeface.BOLD)); return view; }
    private static String difficultyName(int tier) { return new String[]{"very easy", "easy", "medium", "hard", "very hard"}[Math.max(1, Math.min(5, tier)) - 1]; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    static String daysText(int mask) { StringBuilder b = new StringBuilder(); for (int i = 0; i < 7; i++) if ((mask & (1 << i)) != 0) { if (b.length() > 0) b.append(" · "); b.append(DAYS[i]); } return b.toString(); }
}
