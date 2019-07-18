package cn.wildfirechat.proto;
import android.text.TextUtils;

import com.comsince.github.logger.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AndroidLogger implements Log{

    private Class loggerClass;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setTag(Class tagClass) {
        this.loggerClass = tagClass;
    }

    @Override
    public void i(String s, String s1) {
        android.util.Log.i("comsince "+"["+Thread.currentThread().getName()+"]"+" ["+dateFormat.format(new Date())+"] "+"["+loggerClass.getSimpleName()+"] "+s,s1);
    }

    @Override
    public void i(String s) {
        android.util.Log.i("comsince "+"["+Thread.currentThread().getName()+"]"+" ["+dateFormat.format(new Date())+"] "+"["+loggerClass.getSimpleName()+"] ",s);
    }

    @Override
    public void e(String s, Throwable e) {
        String message = "";
        if(e != null){
            message = s+":"+e.getMessage();
        }
        if(TextUtils.isEmpty(message)){
            message = s;
        }
        android.util.Log.e("comsince "+"["+Thread.currentThread().getName()+"]"+" ["+dateFormat.format(new Date())+"] "+"["+loggerClass.getSimpleName()+"] ",message);
    }

    @Override
    public void e(String s, String s1, Exception e) {
        android.util.Log.e("comsince "+"["+Thread.currentThread().getName()+"]"+" ["+dateFormat.format(new Date())+"] "+"["+loggerClass.getSimpleName()+"] ",s,e);
    }

    @Override
    public void e(String e) {
        android.util.Log.e("comsince "+"["+Thread.currentThread().getName()+"]"+" ["+dateFormat.format(new Date())+"] "+"["+loggerClass.getSimpleName()+"] ",e);
    }
}
