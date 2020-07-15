package cn.wildfirechat.log;

import android.content.Context;

public class Logger {
    private static Log sDelegate;

    public static void init(Context context) {
        init(FileLog.Builder.defaultBuilder(context).build());
    }

    public static void init(FileLog delegate) {
        if (sDelegate == null) {
            sDelegate = delegate;
        }
    }

    public static void d(String tag, String msg) {
        if (sDelegate != null) {
            sDelegate.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (sDelegate != null) {
            sDelegate.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (sDelegate != null) {
            sDelegate.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (sDelegate != null) {
            sDelegate.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable throwable) {
        if (sDelegate != null) {
            sDelegate.e(tag, msg, throwable);
        }
    }

    public static void flush(boolean currentThread) {
        if (sDelegate != null) {
            sDelegate.flush(currentThread);
        }
    }
}
