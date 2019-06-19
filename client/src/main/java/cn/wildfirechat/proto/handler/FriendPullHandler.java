package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.logging.Logger;

import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class FriendPullHandler extends AbstractMessagHandler{
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
                workExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        protoService.getMessages(Conversation.ConversationType.Single.ordinal(),friend,0,0,false,0,null);
                    }
                });
            }

            friendListFuture.setComplete(friendList);
            if(friendList.length != 0){
                log.i("update friend list "+friendList[0]);
                protoService.myFriendList = friendList;
                JavaProtoLogic.onFriendListUpdated(friendList);
            }
        } catch (InvalidProtocolBufferException e) {
            friendListFuture.setComplete(new String[0]);
        }
    }
}
