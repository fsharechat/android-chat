package cn.wildfirechat.alarm;

import android.os.Handler;
import android.os.Looper;


public class Schedule {
    private Looper mLooper;
    private Handler mHandler;

    public Schedule(Looper looper) {
        mLooper = looper;
        mHandler = new Handler(mLooper);
    }

    public void post(Runnable r) {
        mHandler.post(r);
    }

    public void postDelayed(Runnable r, long delayMillis) {
        mHandler.postDelayed(r, delayMillis);
    }

    public long threadId() {
        return mLooper.getThread().getId();
    }

    public String name() {
        return mLooper.getThread().getName();
    }

    public Handler handler() {
        return mHandler;
    }

    public enum Type {
        MAIN,
        IO,
        EVENT,
        COMPUTATION
    }
}
