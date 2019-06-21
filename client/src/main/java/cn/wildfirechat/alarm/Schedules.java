package cn.wildfirechat.alarm;

import android.os.HandlerThread;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

public class Schedules {
    private static Map<Schedule.Type, Schedule> sScheduleMap = new HashMap<>();
    private static Schedule sIo, sEvent, sMain, sComputation;

    static HandlerThread newThread(String name) {
        HandlerThread thread = new HandlerThread(name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static synchronized Schedule get(Schedule.Type type) {
        Schedule schedule = sScheduleMap.get(type);
        if (schedule == null) {
            switch (type) {
                case MAIN:
                    schedule = new Schedule(Looper.getMainLooper());
                    break;
                case IO:
                    schedule = new Schedule(newThread("io").getLooper());
                    break;
                case EVENT:
                    schedule = new Schedule(newThread("event").getLooper());
                    break;
                case COMPUTATION:
                    schedule = new Schedule(newThread("computation").getLooper());
                    break;
            }
            sScheduleMap.put(type, schedule);
        }
        return schedule;
    }

    public static Schedule main() {
        if (sMain == null) {
            sMain = get(Schedule.Type.MAIN);
        }
        return sMain;
    }

    public static Schedule io() {
        if (sIo == null) {
            sIo = get(Schedule.Type.IO);
        }
        return sIo;
    }

    public static Schedule event() {
        if (sEvent == null) {
            sEvent = get(Schedule.Type.EVENT);
        }
        return sEvent;
    }

    public static Schedule computation() {
        if (sComputation == null) {
            sComputation = get(Schedule.Type.COMPUTATION);
        }
        return sComputation;
    }
}
