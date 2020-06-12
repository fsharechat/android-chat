package cn.wildfirechat.proto.upload;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;

public class ByteBody implements AsyncHttpRequestBody<byte[]> {
    public static final String CONTENT_TYPE = "application/binary";
    byte[] body;
    String contentType = CONTENT_TYPE;

    public ByteBody(byte[] body) {
        this.body = body;
    }

    @Override
    public void write(AsyncHttpRequest request, DataSink sink, CompletedCallback completed) {
        Util.writeAll(sink,body,completed);
    }

    @Override
    public void parse(DataEmitter emitter, CompletedCallback completed) {
        throw new AssertionError("not implemented");
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean readFullyOnRequest() {
        throw new AssertionError("not implemented");
    }

    @Override
    public int length() {
        return body.length;
    }

    @Override
    public byte[] get() {
        return body;
    }
}
