package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;

import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

public class NotifyFriendHandler extends AbstractMessageHandler {
    private Log log = LoggerFactory.getLogger(NotifyFriendHandler.class);
    public NotifyFriendHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUBLISH == header.getSignal() && SubSignal.FN == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        long unRead = byteBufferList.getLong();
        log.i("receive notifyHandler signal " +unRead);
        protoService.getImMemoryStore().setFriendRequestHead(unRead);
        workExecutor.execute(new Runnable() {
            @Override
            public void run() {
                JavaProtoLogic.onFriendRequestUpdated();
                protoService.getMyFriendList(true);
            }
        });
    }
}
