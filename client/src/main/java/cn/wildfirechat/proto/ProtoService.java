package cn.wildfirechat.proto;
import android.text.TextUtils;

import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import cn.wildfirechat.alarm.AlarmWrapper;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.handler.AddFriendRequestHandler;
import cn.wildfirechat.proto.handler.ConnectAckMessageHandler;
import cn.wildfirechat.proto.handler.CreateGroupHandler;
import cn.wildfirechat.proto.handler.FriendPullHandler;
import cn.wildfirechat.proto.handler.FriendRequestHandler;
import cn.wildfirechat.proto.handler.GetUserInfoMessageHanlder;
import cn.wildfirechat.proto.handler.HandlerFriendRequestHandler;
import cn.wildfirechat.proto.handler.NotifyFriendHandler;
import cn.wildfirechat.proto.handler.NotifyFriendRequestHandler;
import cn.wildfirechat.proto.handler.NotifyMessageHandler;
import cn.wildfirechat.proto.handler.ReceiveMessageHandler;
import cn.wildfirechat.proto.handler.SearchUserResultMessageHandler;
import cn.wildfirechat.proto.handler.SendMessageHandler;
import cn.wildfirechat.proto.store.ImMemoryStore;
import cn.wildfirechat.proto.store.ImMemoryStoreImpl;

public class ProtoService extends AbstractProtoService {

    protected ImMemoryStore imMemoryStore;

    public ProtoService(AlarmWrapper alarmWrapper){
        super(alarmWrapper);
        imMemoryStore = new ImMemoryStoreImpl();
        initHandlers();
    }

    public ImMemoryStore getImMemoryStore(){
        return imMemoryStore;
    }

    private void initHandlers(){
        messageHandlers.add(new ConnectAckMessageHandler(this));
        messageHandlers.add(new SearchUserResultMessageHandler(this));
        messageHandlers.add(new GetUserInfoMessageHanlder(this));
        messageHandlers.add(new AddFriendRequestHandler(this));
        messageHandlers.add(new NotifyFriendHandler(this));
        messageHandlers.add(new FriendRequestHandler(this));
        messageHandlers.add(new HandlerFriendRequestHandler(this));
        messageHandlers.add(new FriendPullHandler(this));
        messageHandlers.add(new SendMessageHandler(this));
        messageHandlers.add(new ReceiveMessageHandler(this));
        messageHandlers.add(new NotifyMessageHandler(this));
        messageHandlers.add(new NotifyFriendRequestHandler(this));
        messageHandlers.add(new CreateGroupHandler(this));
    }

    public void searchUser(String keyword, JavaProtoLogic.ISearchUserCallback callback){
        WFCMessage.SearchUserRequest request = WFCMessage.SearchUserRequest.newBuilder()
                .setKeyword(keyword)
                .setFuzzy(1)
                .setPage(0)
                .build();
        sendMessage(Signal.PUBLISH,SubSignal.US,request.toByteArray(),callback);
    }


    public ProtoUserInfo getUserInfo(String userId, String groupId, boolean refresh){
        WFCMessage.PullUserRequest request = WFCMessage.PullUserRequest.newBuilder()
                .addRequest(WFCMessage.UserRequest.newBuilder().setUid(userId).build())
                .build();
        SimpleFuture<ProtoUserInfo> simpleFuture = sendMessageSync(Signal.PUBLISH,SubSignal.UPUI,request.toByteArray());
        try {
            return simpleFuture.get(200,TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.e("getuserinfo error ",e);
            return null;
        }
    }

    public void sendFriendRequest(String userId, String reason, JavaProtoLogic.IGeneralCallback callback){
        WFCMessage.AddFriendRequest friendRequest = WFCMessage.AddFriendRequest.newBuilder()
                .setReason(reason)
                .setTargetUid(userId)
                .build();
        sendMessage(Signal.PUBLISH, SubSignal.FAR,friendRequest.toByteArray(),callback);
    }

    public int getUnreadFriendRequestStatus(){
        ProtoFriendRequest[] protoFriendRequests = getFriendRequest(true);
        if(protoFriendRequests != null){
            return protoFriendRequests.length;
        } else {
            return 0;
        }
    }

    public ProtoFriendRequest[] getFriendRequest(boolean incomming) {
        WFCMessage.Version version = WFCMessage.Version.newBuilder().setVersion(System.currentTimeMillis() - 60 * 1000).build();
        SimpleFuture<ProtoFriendRequest[]> friendRequestFuture = sendMessageSync(Signal.PUBLISH,SubSignal.FRP,version.toByteArray());
        try {
            return friendRequestFuture.get(200,TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    public void handleFriendRequest(String userId, boolean accept, JavaProtoLogic.IGeneralCallback callback){
        WFCMessage.HandleFriendRequest handleFriendRequest = WFCMessage.HandleFriendRequest.newBuilder()
                .setTargetUid(userId)
                .setStatus(0)
                .build();
        sendMessage(Signal.PUBLISH,SubSignal.FHR,handleFriendRequest.toByteArray(),callback);
    }

    /**
     * 获取用户朋友列表
     * @param refresh 是否强制刷新
     * */
    public String[] getMyFriendList(boolean refresh){
        if(!refresh && imMemoryStore.hasFriend()){
            return imMemoryStore.getFriendListArr();
        }

        WFCMessage.Version request = WFCMessage.Version.newBuilder().setVersion(0).build();
        SimpleFuture<String[]> friendListFuture = sendMessageSync(Signal.PUBLISH,SubSignal.FP,request.toByteArray());
        try {
            friendListFuture.get(500,TimeUnit.MILLISECONDS);
            return imMemoryStore.getFriendListArr();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isMyFriend(String userId){
        return imMemoryStore.isMyFriend(userId);
    }

    public ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser){
        log.i("conversationType "+conversationType+" target "+target+" line "+line +" fromIndex "+fromIndex+" count "+" withuser "+withUser);
        ProtoMessage[] protoMessages = null;
        if(!TextUtils.isEmpty(target)){
            protoMessages = imMemoryStore.getMessages(conversationType,target);
        }

        if(protoMessages == null){
            long targetLastMessageId = imMemoryStore.getTargetLastMessageId(target);
            log.i("targetLastMessageId "+targetLastMessageId);
            WFCMessage.PullMessageRequest pullMessageRequest = WFCMessage.PullMessageRequest.newBuilder()
                    .setId(targetLastMessageId)
                    .setType(conversationType)
                    .build();
            SimpleFuture<ProtoMessage[]> pullMessageFuture = sendMessageSync(Signal.PUBLISH,SubSignal.MP,pullMessageRequest.toByteArray());
            try {
                pullMessageFuture.get(500,TimeUnit.MILLISECONDS);
                protoMessages = imMemoryStore.getMessages(conversationType,target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



        return protoMessages;
    }

    public void getRemoteMessages(int conversationType, String target, int line, long beforeMessageUid, int count, JavaProtoLogic.ILoadRemoteMessagesCallback callback){
        callback.onSuccess(new ProtoMessage[0]);
    }

    public void sendMessage(ProtoMessage msg, int expireDuration, JavaProtoLogic.ISendMessageCallback callback){
        WFCMessage.Message sendMessage = convertWFCMessage(msg);
        imMemoryStore.addProtoMessageByTarget(msg.getTarget(),msg,false);
        sendMessage(Signal.PUBLISH,SubSignal.MS,sendMessage.toByteArray(),callback);
    }


    public void pullMessage(long messageId,int type){
        WFCMessage.PullMessageRequest pullMessageRequest = WFCMessage.PullMessageRequest.newBuilder()
                .setId(messageId)
                .setType(type)
                .build();
        sendMessage(Signal.PUBLISH,SubSignal.MP,pullMessageRequest.toByteArray(),null);
    }


    /**
     * 创建群组
     * */
    public void createGroup(String groupId, String groupName, String groupPortrait, String[] memberIds, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback2 callback){
        WFCMessage.GroupInfo groupInfo = WFCMessage.GroupInfo.newBuilder()
                .setName(groupName)
                .setTargetId(TextUtils.isEmpty(groupId)? "":groupId)
                .setPortrait(TextUtils.isEmpty(groupPortrait)? "":groupPortrait)
                .setType(ProtoConstants.GroupType.GroupType_Free)
                .build();

        WFCMessage.Group.Builder groupBuilder = WFCMessage.Group.newBuilder();
        groupBuilder.setGroupInfo(groupInfo);
        if(memberIds != null){
            for(String memberId : memberIds){
                WFCMessage.GroupMember member = WFCMessage.GroupMember.newBuilder()
                        .setMemberId(memberId)
                        .setType(memberId.equals(getUserName())? ProtoConstants.GroupMemberType.GroupMemberType_Owner: ProtoConstants.GroupMemberType.GroupMemberType_Normal)
                        .build();
                groupBuilder.addMembers(member);
            }
        }
        List<Integer> lines = new ArrayList<>();
        if(notifyLines != null){
            for(int line : notifyLines){
                lines.add(line);
            }
        }
        WFCMessage.CreateGroupRequest.Builder createGroupRequestBuilder = WFCMessage.CreateGroupRequest.newBuilder();
        createGroupRequestBuilder.setGroup(groupBuilder.build());
        WFCMessage.MessageContent messageContent = convert2WfcMessageContent(notifyMsg);
        if(messageContent != null){
            createGroupRequestBuilder.setNotifyContent(messageContent);
        }
        createGroupRequestBuilder.addAllToLine(lines);
        sendMessage(Signal.PUBLISH,SubSignal.GC, createGroupRequestBuilder.build().toByteArray(),callback);
    }




}
