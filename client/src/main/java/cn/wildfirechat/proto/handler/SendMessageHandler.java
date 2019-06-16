package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;

import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

public class SendMessageHandler extends AbstractMessagHandler{

    public SendMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.MS == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
       int errorCode = byteBufferList.get();
       ProtoService.log.i("error code "+errorCode);
        JavaProtoLogic.ISendMessageCallback sendMessageCallback = (JavaProtoLogic.ISendMessageCallback) protoService.requestMap.remove(header.getMessageId()).getCallback();
       if(errorCode == 0){
           long messageId = byteBufferList.getLong();
           long timestamp = byteBufferList.getLong();
           ProtoService.log.i("messageId "+messageId+" timestamp "+timestamp);
           sendMessageCallback.onPrepared(messageId,timestamp);
           sendMessageCallback.onSuccess(messageId,timestamp);
       }
    }
}
