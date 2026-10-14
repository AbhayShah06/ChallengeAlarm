package com.abhayshah.challengealarm;

final class AlarmItem {
    final int id;
    int hour;
    int minute;
    int days;
    int difficulty;
    boolean enabled;

    AlarmItem(int id, int hour, int minute, int days, int difficulty, boolean enabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.days = days;
        this.difficulty = Math.max(1, Math.min(5, difficulty));
        this.enabled = enabled;
    }
}
