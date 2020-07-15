package cn.wildfirechat.log;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

public class GlobalLock {
    private static String tag = "GlobalLock";
    private File mFile;
    private FileOutputStream mFos;
    private FileLock mFileLock;
    private String mDir;


    public GlobalLock(String fileName) {
        mFile = new File(mDir, fileName);
        mDir = Environment
                .getExternalStorageDirectory().getAbsolutePath() +
                "/Android/data/global-locks";
    }

    public GlobalLock(String dir, String fileName) {
        this(fileName);
        mDir = dir;
    }

    public GlobalLock(File file) {
        mFile = file;
    }

    private void createFile() throws IOException {
        if (!mFile.exists()) {
            File dir = mFile.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            if (!mFile.exists()) {
                mFile.createNewFile();
            }
        }
    }

    public void lock() {
        try {
            createFile();
            mFos = new FileOutputStream(mFile, true);
            mFileLock = mFos.getChannel().lock();
        } catch (IOException e) {
            Log.e(tag, "lock", e);
        }
    }

    public void release() {
        if (mFileLock != null) {
            try {
                mFileLock.release();
            } catch (IOException e) {
                Log.e(tag, "release", e);
            } finally {
                if (mFos != null) {
                    try {
                        mFos.close();
                    } catch (IOException e) {
                        Log.e(tag, "close", e);
                    }
                }
            }
        }
    }
}
