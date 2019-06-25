package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class NotifyMessageHandler extends AbstractMessageHandler {

    public NotifyMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUBLISH == header.getSignal() && SubSignal.MN == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        ProtoService.log.i("receiver subsignal "+header.getSubSignal());
        try {
            WFCMessage.NotifyMessage notifyMessage = WFCMessage.NotifyMessage.parseFrom(byteBufferList.getAllByteArray());
            ProtoService.log.i("notifymessage "+notifyMessage.getHead()+" "+notifyMessage.getTarget()+" type "+notifyMessage.getType());
            protoService.pullMessage(notifyMessage.getHead()-1,notifyMessage.getType());

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
