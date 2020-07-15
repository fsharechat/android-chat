package cn.wildfirechat.log;

import android.text.TextUtils;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

class LogWriter {
    private String mDir;
    private static final SimpleDateFormat sDataFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final String sSuffixName = ".log.txt";
    private int count = 7;
    private boolean mGlobalLock;

    public LogWriter(String dir) {
        mDir = dir;
    }

    public LogWriter(String dir, boolean globalLock) {
        mDir = dir;
        mGlobalLock = globalLock;
    }

    private void checkFileCount(File dir) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name != null && name.endsWith(sSuffixName);
            }
        });
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long diff = o1.lastModified() - o2.lastModified();
                return diff > 0 ? -1 :
                        diff == 0 ? 0 : 1;
            }
        });
        for (int index = count; index < files.length; ++index) {
            files[index].delete();
        }
    }

    public void write(List<String> logs) throws IOException {
        if (!TextUtils.isEmpty(mDir)) {
            File dir = new File(mDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            File logFile = new File(dir, sDataFormat.format(new Date()) + sSuffixName);
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    checkFileCount(dir);
                } else {
                    return;
                }
            }
            GlobalLock globalLock = null;
            if (mGlobalLock) {
                globalLock = new GlobalLock(logFile);
                globalLock.lock();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            try {
                for (String log : logs) {
                    writer.write(log + "\r\n");
                }
            } finally {
                if (mGlobalLock && globalLock != null) {
                    globalLock.release();
                }
                writer.close();
            }
        }
    }
}

