package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.proto.JavaProtoLogic;
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
                    //如果lastMessageSeq为0表示初始用户下拉消息，其他为连接获取未读消息
                    if(lastMessageSeq != 0){
                        protoService.pullMessage(lastMessageSeq,0);
                    } else {
                        protoService.pullMessage(lastMessageSeq, 0, new JavaProtoLogic.IGeneralCallback() {
                            @Override
                            public void onSuccess() {
                                logger.i("connect pull message success");
                            }

                            @Override
                            public void onFailure(int errorCode) {
                                logger.i("connect pull message fail "+errorCode);
                            }
                        });
                    }

                }
            });
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
