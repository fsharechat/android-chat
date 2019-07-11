package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.google.protobuf.InvalidProtocolBufferException;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class ConnectAckMessageHandler extends AbstractMessageHandler {
    private static final String TAG = "ConnectAckMessage";

    public ConnectAckMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return header.getSignal() == Signal.CONNECT_ACK;
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        try {
            WFCMessage.ConnectAckPayload connectAckPayload = WFCMessage.ConnectAckPayload.parseFrom(byteBufferList.getAllByteArray());
            long lastMessageSeq = protoService.getImMemoryStore().getLastMessageSeq();
            logger.i(TAG,"friendHead "+connectAckPayload.getFriendHead()+" msgHead "+connectAckPayload.getMsgHead()+" lastMessageSeq "+lastMessageSeq+" serverTime "+connectAckPayload.getServerTime());
            workExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    protoService.getMyFriendList(true);
                    protoService.pullMessage(lastMessageSeq,0);
                }
            });
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
