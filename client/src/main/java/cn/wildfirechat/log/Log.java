package cn.wildfirechat.log;

public interface Log {
    void d(String tag, String msg);
    void i(String tag, String msg);
    void w(String tag, String msg);
    void e(String tag, String msg);
    void e(String tag, String msg, Throwable throwable);
    void flush(boolean currentThread);
}
