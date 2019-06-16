package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class ReceiveMessageHandler extends AbstractMessagHandler {

    public ReceiveMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.MP == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        ProtoService.log.i("receive message code "+errorCode);
        if(errorCode == 0){
            try {
                WFCMessage.PullMessageResult pullMessageResult = WFCMessage.PullMessageResult.parseFrom(byteBufferList.getAllByteArray());

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }
}
