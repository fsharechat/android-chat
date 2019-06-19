package cn.wildfirechat.proto.handler;

import android.util.Log;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class ConnectAckMessageHandler extends AbstractMessagHandler{
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
            workExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    protoService.getMyFriendList(true);
                    protoService.getMessages(Conversation.ConversationType.Single.ordinal(),null,0,0,false,0,null);

                }
            });
            JavaProtoLogic.onConnectionStatusChanged(ConnectionStatus.ConnectionStatusConnected);
            WFCMessage.ConnectAckPayload connectAckPayload = WFCMessage.ConnectAckPayload.parseFrom(byteBufferList.getAllByteArray());
            Log.i(TAG,"friendHead "+connectAckPayload.getFriendHead()+" msgHead "+connectAckPayload.getMsgHead()+" serverTime "+connectAckPayload.getServerTime());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
