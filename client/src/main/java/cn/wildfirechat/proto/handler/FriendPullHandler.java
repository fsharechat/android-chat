package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class FriendPullHandler extends AbstractMessageHandler {
    private Log log = LoggerFactory.getLogger(FriendPullHandler.class);
    public FriendPullHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.FP == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        log.i("FriendPullHandler error code "+errorCode);
        SimpleFuture<String[]> friendListFuture = protoService.futureMap.remove(header.getMessageId());
        try {
            WFCMessage.GetFriendsResult getFriendsResult = WFCMessage.GetFriendsResult.parseFrom(byteBufferList.getAllByteArray());
            String[] friendList = new String[getFriendsResult.getEntryCount()];
            for(int i = 0;i < getFriendsResult.getEntryCount(); i++){
                friendList[i] = getFriendsResult.getEntry(i).getUid();
                String friend = friendList[i];
                log.i("friend list "+friend);
            }

            friendListFuture.setComplete(friendList);
            if(friendList.length != 0){
                protoService.getImMemoryStore().setFriendArr(friendList,true);
                JavaProtoLogic.onFriendListUpdated(friendList);
            }
        } catch (InvalidProtocolBufferException e) {
            friendListFuture.setComplete(new String[0]);
        }
    }
}
