package cn.wildfirechat.alarm;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AlarmWrapper {

    Log log = LoggerFactory.getLogger(AlarmWrapper.class);

    private static final String tag = "AlarmWrapper";
    private static final String scheme = "timer";
    private Context mAppContext;
    private String mAuthority;
    private int mKeySequence = 1;
    private AlarmManager mAlarmManager;
    private Map<Integer, Timer> mTimerMap = new HashMap<>();
    private TimerReceiver mTimerReceiver;

    public AlarmWrapper(Context context, String authority) {
        mAppContext = context.getApplicationContext();
        mAuthority = authority;
    }

    public void start() {
        log.i(tag, "start with " + mAuthority + " Android " + Build.VERSION.SDK_INT);
        mAlarmManager = (AlarmManager)
                mAppContext.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mTimerReceiver = new TimerReceiver();
            IntentFilter filter = new IntentFilter(mAuthority);
            filter.addDataScheme(scheme);
            mAppContext.registerReceiver(mTimerReceiver, filter);
        }
    }

    public void stop() {
        log.i(tag, "stop with " + mAuthority);
        if (mTimerReceiver != null) {
            mAppContext.unregisterReceiver(mTimerReceiver);
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Integer, Timer> entry : mTimerMap.entrySet()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mAlarmManager.cancel(entry.getValue().onAlarmListener);
                    } else {
                        mAlarmManager.cancel(entry.getValue().pendingIntent);
                    }
                }
                mTimerMap.clear();
            }
        };
        if (Thread.currentThread().getId() == Schedules.event().threadId()) {
            task.run();
        } else {
            Schedules.event().post(task);
        }
    }

    private PendingIntent pendingIntent(int key) {
        Intent intent = new Intent(mAuthority);
        intent.setData(Uri.parse(scheme + "://" + mAuthority + "/" + key));
        return PendingIntent.getBroadcast(mAppContext,
                0, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private synchronized int key() {
        if (mKeySequence == 0) {
            ++mKeySequence;
        }
        return mKeySequence++;
    }

    public void schedule(final Timer timer) {
        log.i(tag, "schedule " + timer);
        if (timer != null && timer.key == 0) {
            timer.key = key();
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    mTimerMap.put(timer.key, timer);
                    int type = timer.wakeup ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mAlarmManager.setExact(type,
                                System.currentTimeMillis() + timer.period,
                                null, new TimerListener(timer), timer.schedule.handler());
                    } else {
                        timer.pendingIntent = pendingIntent(timer.key);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            mAlarmManager.setExact(type,
                                    System.currentTimeMillis() + timer.period,
                                    timer.pendingIntent);
                        } else {
                            mAlarmManager.set(type,
                                    System.currentTimeMillis() + timer.period,
                                    timer.pendingIntent);
                        }
                    }
                }
            };
            if (Thread.currentThread().getId() == Schedules.event().threadId()) {
                task.run();
            } else {
                Schedules.event().post(task);
            }
        }
    }

    public void cancel(final Timer timer) {
        log.i(tag, "cancel " + timer);
        if (timer != null && timer.key != 0) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    if (mTimerMap.remove(timer.key) != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            mAlarmManager.cancel(timer.onAlarmListener);
                        } else {
                            mAlarmManager.cancel(timer.pendingIntent);
                        }
                    }
                }
            };
            if (Thread.currentThread().getId() == Schedules.event().threadId()) {
                task.run();
            } else {
                Schedules.event().post(task);
            }
        }
    }

    private class TimerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent != null) {
                log.i(tag, "on receive timer broadcast...");
                Runnable action = new Runnable() {
                    @Override
                    public void run() {
                        int key = Integer.parseInt(intent.getData().getLastPathSegment());
                        final Timer timer = mTimerMap.remove(key);
                        if (timer != null && timer.action != null) {
                            if (timer.schedule.threadId() == Thread.currentThread().getId()) {
                                timer.action.run();
                            } else {
                                timer.schedule.post(timer.action);
                            }
                        }
                    }
                };
                if (Thread.currentThread().getId() == Schedules.event().threadId()) {
                    action.run();
                } else {
                    Schedules.event().post(action);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private class TimerListener implements
            AlarmManager.OnAlarmListener, Runnable {
        private Timer mTimer;

        public TimerListener(Timer timer) {
            mTimer = timer;
            mTimer.onAlarmListener = this;
        }

        @Override
        public void run() {
            if (mTimerMap.remove(mTimer.key) != null) {
                if (Thread.currentThread().getId() == mTimer.schedule.threadId()) {
                    mTimer.action.run();
                } else {
                    mTimer.schedule.post(mTimer.action);
                }
            }
        }

        @Override
        public void onAlarm() {
            log.i(tag, "on alarm listener invoke...");
            if (Thread.currentThread().getId() == Schedules.event().threadId()) {
                run();
            } else {
                Schedules.event().post(this);
            }
        }
    }
}
