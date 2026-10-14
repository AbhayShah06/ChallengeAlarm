package com.abhayshah.challengealarm;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import java.util.Calendar;

public class AlarmStoreTest {
    @Test public void oneTimeAlarmChoosesNextFutureOccurrence() {
        Calendar now = Calendar.getInstance(); now.set(2026, Calendar.OCTOBER, 14, 7, 30, 0); now.set(Calendar.MILLISECOND, 0);
        AlarmItem item = new AlarmItem(1, 7, 30, 0, 2, true);
        long next = AlarmStore.nextTime(item, now.getTimeInMillis());
        Calendar result = Calendar.getInstance(); result.setTimeInMillis(next);
        assertTrue(next > now.getTimeInMillis());
        assertTrue(result.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR));
    }

    @Test public void weeklyAlarmChoosesNextSelectedWeekday() {
        Calendar now = Calendar.getInstance(); now.set(2026, Calendar.OCTOBER, 14, 8, 0, 0); now.set(Calendar.MILLISECOND, 0); // Wednesday
        AlarmItem item = new AlarmItem(2, 7, 0, 1 << (Calendar.FRIDAY - Calendar.SUNDAY), 2, true);
        Calendar result = Calendar.getInstance(); result.setTimeInMillis(AlarmStore.nextTime(item, now.getTimeInMillis()));
        assertTrue(result.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);
        assertTrue(result.getTimeInMillis() > now.getTimeInMillis());
    }
}
