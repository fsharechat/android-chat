package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

public class AddFriendRequestHandler extends AbstractMessageHandler {
    Log log = LoggerFactory.getLogger(AddFriendRequestHandler.class);

    public AddFriendRequestHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.FAR == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCocde = byteBufferList.get();
        log.i("add friend request error code "+errorCocde);
        RequestInfo requestInfo = protoService.requestMap.remove(header.getMessageId());
        JavaProtoLogic.IGeneralCallback generalCallback = (JavaProtoLogic.IGeneralCallback) requestInfo.getCallback();
        if(errorCocde == 0){
            generalCallback.onSuccess();
        } else {
            generalCallback.onFailure(errorCocde);
        }
    }
}
