package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;

public interface MessageHandler {
    boolean match(Signal signal);
    void processMessage(Signal signal, SubSignal subSignal, ByteBufferList byteBufferList);
}
