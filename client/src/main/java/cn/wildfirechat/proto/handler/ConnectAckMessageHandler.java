package cn.wildfirechat.proto.handler;

import android.util.Log;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.proto.WFCMessage;

public class ConnectAckMessageHandler implements MessageHandler{
    private static final String TAG = "ConnectAckMessage";
    @Override
    public boolean match(Header header) {
        return header.getSignal() == Signal.CONNECT_ACK;
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        try {
            WFCMessage.ConnectAckPayload connectAckPayload = WFCMessage.ConnectAckPayload.parseFrom(byteBufferList.getAllByteArray());
            Log.i(TAG,"friendHead "+connectAckPayload.getFriendHead()+" msgHead "+connectAckPayload.getMsgHead()+" serverTime "+connectAckPayload.getServerTime());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
