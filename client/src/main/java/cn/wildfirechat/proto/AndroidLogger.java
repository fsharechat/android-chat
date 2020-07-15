package cn.wildfirechat.proto;
import android.content.Context;
import android.text.TextUtils;

import com.comsince.github.logger.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.wildfirechat.log.Logger;

public class AndroidLogger implements Log{

    private Class loggerClass;

    public AndroidLogger(Context context){
        Logger.init(context);
    }

    @Override
    public void setTag(Class tagClass) {
        this.loggerClass = tagClass;
    }

    @Override
    public void i(String s, String s1) {
        Logger.i(loggerClass.getSimpleName(),s+" "+s1);
    }

    @Override
    public void i(String s) {
        Logger.i(loggerClass.getSimpleName(),s);
    }

    @Override
    public void e(String s, Throwable e) {
        Logger.e(loggerClass.getSimpleName(),s,e);
    }

    @Override
    public void e(String s, String s1, Exception e) {
        Logger.e(loggerClass.getSimpleName(),s+" "+s1,e);
    }

    @Override
    public void e(String e) {
        Logger.e(loggerClass.getSimpleName(),e);
    }
}
