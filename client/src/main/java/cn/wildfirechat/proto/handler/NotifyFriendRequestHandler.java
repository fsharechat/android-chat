package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;

import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

public class NotifyFriendRequestHandler extends AbstractMessageHandler {

    public NotifyFriendRequestHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUBLISH == header.getSignal() && SubSignal.FRN == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        ProtoService.log.i("receive friend request update message");
        long unRead = byteBufferList.getLong();
        protoService.getImMemoryStore().setFriendRequestHead(unRead - 1);
        JavaProtoLogic.onFriendRequestUpdated();
    }
}
