package cn.wildfirechat.log;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.wildfirechat.alarm.Schedules;

public class FileLog implements Log{
    private static int sPid = Process.myPid();
    private String mDir;
    private boolean mDebug;
    private boolean mGlobalLock;
    private int mCacheCount;
    private long mCacheDuration;
    private List<LogInfo> mLogInfoList = new ArrayList<>();
    private Handler mLogHandler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");

    private FileLog(String dir,
                      boolean debug,
                      boolean globalLock,
                      int cacheCount,
                      long cacheDuration) {
        mDir = dir;
        mDebug = debug;
        mGlobalLock = globalLock;
        mCacheCount = cacheCount;
        mCacheDuration = cacheDuration;
    }

    private void write() {
        List<LogInfo> copiedLogInfo = null;
        try {
            mLogHandler.removeCallbacksAndMessages(null);
            synchronized (mLogInfoList) {
                copiedLogInfo = new ArrayList<>(mLogInfoList);
                mLogInfoList.clear();
            }
            List<String> logs = new ArrayList<>();
            for (LogInfo logInfo : copiedLogInfo) {
                logs.add(logInfo.toString());
            }
            LogWriter writer = new LogWriter(mDir, mGlobalLock);
            writer.write(logs);
        } catch (Throwable throwable) {
            //ignore
        } finally {
            if (copiedLogInfo != null) {
                copiedLogInfo.clear();
            }
        }
    }

    private void process(LogInfo logInfo) {
        synchronized (mLogInfoList) {
            mLogInfoList.add(logInfo);
            if (mLogInfoList.size() == 1) {
                mLogHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        flush(false);
                    }
                }, mCacheDuration);
            }
            if (mLogInfoList.size() == mCacheCount) {
                mLogHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        flush(false);
                    }
                });
            }
        }
    }

    @Override
    public void d(String tag, String msg) {
        if (mDebug) {
            android.util.Log.d(tag, msg);
        }
        process(new LogInfo("D", tag, msg));
    }

    @Override
    public void i(String tag, String msg) {
        if (mDebug) {
            android.util.Log.i(tag, msg);
        }
        process(new LogInfo("I", tag, msg));
    }

    @Override
    public void w(String tag, String msg) {
        if (mDebug) {
            android.util.Log.w(tag, msg);
        }
        process(new LogInfo("W", tag, msg));
    }

    @Override
    public void e(String tag, String msg) {
        if (mDebug) {
            android.util.Log.e(tag, msg);
        }
        process(new LogInfo("E", tag, msg));
    }

    @Override
    public void e(String tag, String msg, Throwable throwable) {
        if (mDebug) {
            android.util.Log.e(tag, msg, throwable);
        }
        process(new LogInfo("E", tag, msg + "\r\n" + android.util.Log.getStackTraceString(throwable)));
    }

    @Override
    public void flush(boolean currentThread) {
        if (currentThread) {
            write();
        } else {
            Schedules.io().post(new Runnable() {
                @Override
                public void run() {
                    write();
                }
            });
        }
    }

    private class LogInfo {
        String time;
        long tid;
        String level;
        String tag;
        String msg;

        LogInfo(String level, String tag, String msg) {
            this.time = mDateFormat.format(new Date());
            this.tid = Thread.currentThread().getId();
            this.level = level;
            this.tag = tag;
            this.msg = msg;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(time)
                    .append(" ")
                    .append(sPid)
                    .append("-")
                    .append(tid)
                    .append(" ")
                    .append(level)
                    .append("/")
                    .append(tag)
                    .append(" ")
                    .append(msg);
            String data = builder.toString();
            return data;
        }
    }

    @Override
    public String toString() {
        return "CryptoLog{" +
                "mDir='" + mDir + '\'' +
                ", mDebug=" + mDebug +
                ", mCacheCount=" + mCacheCount +
                ", mCacheDuration=" + mCacheDuration +
                '}';
    }

    public static class Builder {
        private Context mContext;
        private String mDir;
        private boolean mDebug = false;
        private boolean mGlobalLock = false;
        private int mCacheCount = 10;
        private long mCacheDuration = 180 * 1000;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setDir(String dir) {
            mDir = getDirPrefix(mContext)+ dir;
            return this;
        }

        public Builder setDebug(boolean debug) {
            mDebug = debug;
            return this;
        }

        public Builder setGlobalLock(boolean enable) {
            mGlobalLock = enable;
            return this;
        }

        public Builder setCacheCount(int cacheCount) {
            mCacheCount = cacheCount;
            return this;
        }

        public Builder setCacheDuration(long duration) {
            mCacheDuration = duration;
            return this;
        }

        public FileLog build() {
            return new FileLog(mDir, mDebug, mGlobalLock, mCacheCount, mCacheDuration);
        }

        public static Builder defaultBuilder(Context context) {
            Builder builder = new Builder(context);
            String dir = Package.processName(context);
            if (TextUtils.isEmpty(dir)) {
                dir = context.getPackageName();
            }
            builder.mDir = getDirPrefix(context) + dir;
            builder.mDebug = Package.debuggable(context);
            return builder;
        }

        private static String getDirPrefix(Context context) {
            String path;
            if (Build.VERSION.SDK_INT >= 29) {
                path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath() + "/";
            } else {
                path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/";
            }
            return path;
        }
    }
}
