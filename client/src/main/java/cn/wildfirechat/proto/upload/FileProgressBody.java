package cn.wildfirechat.proto.upload;

import android.util.Log;

import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.FileBody;

import java.io.File;

public class FileProgressBody extends FileBody {

    UploadProgressPercentHandler uploadProgressPercentHandler;
    public FileProgressBody(File file,UploadProgressPercentHandler uploadProgressPercentHandler) {
        super(file);
        this.uploadProgressPercentHandler = uploadProgressPercentHandler;
    }

    @Override
    public void write(AsyncHttpRequest request, DataSink sink, CompletedCallback completed) {
        UploadProgressUtils.pump(get(), sink, completed, new UploadProgressHandler() {
            @Override
            public void progress(int upload) {
                uploadProgressPercentHandler.progress((double) upload / length());
            }
        });
    }
}
