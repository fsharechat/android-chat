package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;

public interface MessageHandler {
    boolean match(Header header);
    void processMessage(Header header, ByteBufferList byteBufferList);
}
