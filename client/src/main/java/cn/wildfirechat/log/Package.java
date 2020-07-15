package cn.wildfirechat.log;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Package {
    private static Boolean sDebuggable = null;

    public static Version version(Context context) {
        return version(context, null, 0);
    }

    public static Version version(Context context, String packageName, int flags) {
        Version version = new Version();
        if (TextUtils.isEmpty(packageName)) {
            packageName = context.getPackageName();
        }
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, flags);
            version.name = info.versionName;
            version.code = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            //ignore
        }
        return version;
    }

    public static String processName(Context context) {
        String processName = null;
        ActivityManager am = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        int pid = android.os.Process.myPid();
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            if (processInfos != null) {
                for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
                    if (process.pid == pid) {
                        processName = process.processName;
                        break;
                    }
                }
            }
        }
        if (processName == null || processName.length() == 0) {
            BufferedReader reader = null;
            try {
                File file = new File("/proc/" + pid + "/cmdline");
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                processName = reader.readLine();
                if (processName != null) {
                    processName = processName.trim();
                }
            } catch (Exception e) {
                Logger.e("Package", "", e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
        return processName;
    }

    public static boolean debuggable(Context context) {
        if (sDebuggable == null) {
            sDebuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        }
        return sDebuggable;
    }

    public static class Version {
        public String name;
        public int code = -1;

        @Override
        public String toString() {
            return "Version{" +
                    "name='" + name + '\'' +
                    ", code=" + code +
                    '}';
        }
    }
}
