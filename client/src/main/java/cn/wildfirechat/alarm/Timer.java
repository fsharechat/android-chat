package cn.wildfirechat.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.text.TextUtils;

public class Timer {
    int key;
    long period;
    boolean wakeup;
    Runnable action;
    Schedule schedule;
    PendingIntent pendingIntent;
    AlarmManager.OnAlarmListener onAlarmListener;
    String toString;

    @Override
    public String toString() {
        if (TextUtils.isEmpty(toString)) {
            toString = "Timer{" +
                    "key=" + key +
                    ", period=" + period +
                    ", wakeup=" + wakeup +
                    ", action=" + action +
                    ", schedule=" + schedule +
                    '}';
        }
        return toString;
    }

    Timer(long period,
          boolean wakeup,
          Schedule schedule,
          Runnable action) {
        this.period = period;
        this.wakeup = wakeup;
        this.action = action;
        this.schedule = (schedule == null ? Schedules.event() : schedule);
    }

    public static class Builder {
        private long period;
        private boolean wakeup;
        private Runnable action;
        private Schedule schedule;

        public Builder period(long period) {
            this.period = period;
            return this;
        }

        public Builder wakeup(boolean wakeup) {
            this.wakeup = wakeup;
            return this;
        }

        public Builder schedule(Schedule schedule) {
            this.schedule = schedule;
            return this;
        }

        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

        public Timer build() {
            return new Timer(period, wakeup, schedule, action);
        }
    }
}
