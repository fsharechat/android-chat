package cn.wildfirechat.proto.handler;

import android.util.Log;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.proto.WFCMessage;

public class PublishAckMessageHandler implements MessageHandler{
    private static final String TAG = "PublishAckHandler";
    @Override
    public boolean match(Signal signal) {
        return signal == Signal.PUB_ACK;
    }

    @Override
    public void processMessage(Signal signal, SubSignal subSignal, ByteBufferList byteBufferList) {
        Log.i(TAG,"user content "+byteBufferList.remaining());
        int errorCode = byteBufferList.get();
        Log.i(TAG,"error code "+errorCode);
        Log.i(TAG,"user content "+byteBufferList.remaining());
        try {
            WFCMessage.SearchUserResult userResult = WFCMessage.SearchUserResult.parseFrom(byteBufferList.getAllByteArray());
            Log.i(TAG,"receive error code "+errorCode+" user "+userResult.getEntryCount());
            for(WFCMessage.User user : userResult.getEntryList()){
                Log.i(TAG,"user "+user.getDisplayName());
            }

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
