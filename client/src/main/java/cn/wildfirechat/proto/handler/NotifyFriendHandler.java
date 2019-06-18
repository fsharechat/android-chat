package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;

import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

public class NotifyFriendHandler extends AbstractMessagHandler{

    public static long friendRequestTime = 0;

    public NotifyFriendHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUBLISH == header.getSignal() && SubSignal.FN == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        //friendRequestTime = byteBufferList.getLong();
        //ProtoService.log.i("receive friend request update message friend request time "+friendRequestTime);
//        JavaProtoLogic.onFriendRequestUpdated();
//        String[] friendList = protoService.getMyFriendList(true);
//        if(friendList != null){
//            JavaProtoLogic.onFriendListUpdated(friendList);
//        }
    }
}
