package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;

import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

public class CreateGroupHandler extends AbstractMessageHandler{

    public CreateGroupHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.GC == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        logger.i("CreateGroupHandler receive errorCode "+errorCode);
        RequestInfo requestInfo = protoService.requestMap.remove(header.getMessageId());
        JavaProtoLogic.IGeneralCallback2 callback2 = (JavaProtoLogic.IGeneralCallback2) requestInfo.getCallback();
        if(errorCode == 0){
            String groupId = byteBufferList.readString();
            logger.i("create group id "+groupId);
            callback2.onSuccess(groupId);
        } else {
            callback2.onFailure(errorCode);
        }
    }
}
