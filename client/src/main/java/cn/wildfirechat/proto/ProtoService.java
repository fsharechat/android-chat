package cn.wildfirechat.proto;
import android.text.TextUtils;

import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import cn.wildfirechat.alarm.AlarmWrapper;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.handler.AddFriendRequestHandler;
import cn.wildfirechat.proto.handler.AddGroupMemberHandler;
import cn.wildfirechat.proto.handler.ConnectAckMessageHandler;
import cn.wildfirechat.proto.handler.CreateGroupHandler;
import cn.wildfirechat.proto.handler.FriendPullHandler;
import cn.wildfirechat.proto.handler.FriendRequestHandler;
import cn.wildfirechat.proto.handler.GetUserInfoMessageHanlder;
import cn.wildfirechat.proto.handler.GroupInfoHandler;
import cn.wildfirechat.proto.handler.GroupMemberHandler;
import cn.wildfirechat.proto.handler.HandlerFriendRequestHandler;
import cn.wildfirechat.proto.handler.KickoffMembersHandler;
import cn.wildfirechat.proto.handler.ModifyGroupInfoHandler;
import cn.wildfirechat.proto.handler.ModifyMyInfoHandler;
import cn.wildfirechat.proto.handler.NotifyFriendHandler;
import cn.wildfirechat.proto.handler.NotifyFriendRequestHandler;
import cn.wildfirechat.proto.handler.NotifyMessageHandler;
import cn.wildfirechat.proto.handler.QuitGroupHandler;
import cn.wildfirechat.proto.handler.ReceiveMessageHandler;
import cn.wildfirechat.proto.handler.SearchUserResultMessageHandler;
import cn.wildfirechat.proto.handler.SendMessageHandler;
import cn.wildfirechat.proto.store.ImMemoryStore;
import cn.wildfirechat.proto.store.ImMemoryStoreImpl;

public class ProtoService extends AbstractProtoService {


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
        messageHandlers.add(new GroupInfoHandler(this));
        messageHandlers.add(new GroupMemberHandler(this));
        messageHandlers.add(new AddGroupMemberHandler(this));
        messageHandlers.add(new KickoffMembersHandler(this));
        messageHandlers.add(new QuitGroupHandler(this));
        messageHandlers.add(new ModifyGroupInfoHandler(this));
        messageHandlers.add(new ModifyMyInfoHandler(this));
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
        log.i("getUserInfo userId "+userId+" groupId "+groupId);
        if(imMemoryStore.getUserInfo(userId) == null || refresh){
            WFCMessage.PullUserRequest request = WFCMessage.PullUserRequest.newBuilder()
                    .addRequest(WFCMessage.UserRequest.newBuilder().setUid(userId).build())
                    .build();
            SimpleFuture<ProtoUserInfo[]> simpleFuture = sendMessageSync(Signal.PUBLISH,SubSignal.UPUI,request.toByteArray());
            try {
                simpleFuture.get(500,TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.e("getuserinfo error ",e);
                return null;
            }
        }
       return imMemoryStore.getUserInfo(userId);
    }

    public void modifyMyInfo(Map<Integer, String> values, JavaProtoLogic.IGeneralCallback callback){
        WFCMessage.ModifyMyInfoRequest.Builder modifyMyInfoBuilder = WFCMessage.ModifyMyInfoRequest.newBuilder();
        for(Map.Entry<Integer,String> entry: values.entrySet()){
            WFCMessage.InfoEntry infoEntry = WFCMessage.InfoEntry.newBuilder().setType(entry.getKey()).setValue(entry.getValue()).build();
            modifyMyInfoBuilder.addEntry(infoEntry);
        }
        sendMessage(Signal.PUBLISH,SubSignal.MMI,modifyMyInfoBuilder.build().toByteArray(),callback);
    }

    public ProtoUserInfo[] getUserInfos(String[] userIds, String groupId){
        log.i("getUserInfos "+Arrays.toString(userIds)+" groupId "+groupId);
        if(imMemoryStore.getUserInfos(userIds) == null){
            WFCMessage.PullUserRequest.Builder userRequestBuilder = WFCMessage.PullUserRequest.newBuilder();
            for(String user : userIds){
                WFCMessage.UserRequest userRequest = WFCMessage.UserRequest.newBuilder().setUid(user).build();
                userRequestBuilder.addRequest(userRequest);
            }
            SimpleFuture<ProtoUserInfo[]> simpleFuture = sendMessageSync(Signal.PUBLISH,SubSignal.UPUI,userRequestBuilder.build().toByteArray());
            try {
                simpleFuture.get(200,TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.e("getuserinfo error ",e);
            }
        }

        return imMemoryStore.getUserInfos(userIds);
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
        if(imMemoryStore.getIncomingFriendRequest().length == 0){
            WFCMessage.Version version = WFCMessage.Version.newBuilder().setVersion(imMemoryStore.getFriendRequestHead()).build();
            SimpleFuture<ProtoFriendRequest[]> friendRequestFuture = sendMessageSync(Signal.PUBLISH,SubSignal.FRP,version.toByteArray());
            try {
                friendRequestFuture.get(200,TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imMemoryStore.getIncomingFriendRequest();

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
            return friendListFuture.get(500,TimeUnit.MILLISECONDS);
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
                if(!TextUtils.isEmpty(target)){
                    protoMessages = imMemoryStore.getMessages(conversationType,target);
                }
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
        log.i("pullMessageId "+messageId);
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
                .setType(ProtoConstants.GroupType.GroupType_Normal)
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

    public ProtoGroupInfo getGroupInfo(String groupId, boolean refresh){
        log.i("getGroupInfo "+groupId+" refresh "+refresh);
        if(refresh || imMemoryStore.getGroupInfo(groupId) == null){
            WFCMessage.PullUserRequest.Builder pullUserRequestBuilder = WFCMessage.PullUserRequest.newBuilder();
            WFCMessage.UserRequest userRequest = WFCMessage.UserRequest.newBuilder().setUid(groupId).build();
            pullUserRequestBuilder.addRequest(userRequest);
            SimpleFuture<ProtoGroupInfo> getGroupInfofuture = sendMessageSync(Signal.PUBLISH,SubSignal.GPGI,pullUserRequestBuilder.build().toByteArray());
            try {
                getGroupInfofuture.get(500,TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imMemoryStore.getGroupInfo(groupId);
    }


    public ProtoGroupMember[] getGroupMembers(String groupId, boolean forceUpdate){
        log.i("getGroupMembers "+groupId+" forceUpdate "+forceUpdate);
        if(forceUpdate || imMemoryStore.getGroupMembers(groupId) == null){
            WFCMessage.PullGroupMemberRequest.Builder groupMemberBuilder = WFCMessage.PullGroupMemberRequest.newBuilder();
            groupMemberBuilder.setTarget(groupId);
            groupMemberBuilder.setHead(0);
            SimpleFuture<ProtoGroupMember[]> simpleFuture = sendMessageSync(Signal.PUBLISH,SubSignal.GPGM,groupMemberBuilder.build().toByteArray());
            try {
                ProtoGroupMember[] protoGroupMembers = simpleFuture.get(500,TimeUnit.MILLISECONDS);
                imMemoryStore.addGroupMember(groupId,protoGroupMembers);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imMemoryStore.getGroupMembers(groupId);
    }


    public ProtoGroupMember getGroupMember(String groupId, String memberId){
        return imMemoryStore.getGroupMember(groupId,memberId);
    }

    public void addMembers(String groupId, String[] memberIds, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
        log.i("groupId "+groupId +" memberIds "+memberIds);
        WFCMessage.AddGroupMemberRequest.Builder memberRequestBuilder = WFCMessage.AddGroupMemberRequest.newBuilder();
        memberRequestBuilder.setGroupId(groupId);
        for(String memberId : memberIds){
            WFCMessage.GroupMember groupMember = WFCMessage.GroupMember.newBuilder()
                    .setMemberId(memberId)
                    .setType(ProtoConstants.GroupMemberType.GroupMemberType_Normal)
                    .build();
            memberRequestBuilder.addAddedMember(groupMember);
        }
        sendMessage(Signal.PUBLISH,SubSignal.GAM,memberRequestBuilder.build().toByteArray(),callback);
    }

    public void kickoffMembers(String groupId, String[] memberIds, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
        WFCMessage.RemoveGroupMemberRequest.Builder removeGroupMemberRequest = WFCMessage.RemoveGroupMemberRequest.newBuilder();
        removeGroupMemberRequest.setGroupId(groupId);
        for(String memberId : memberIds){
            removeGroupMemberRequest.addRemovedMember(memberId);
        }
        sendMessage(Signal.PUBLISH,SubSignal.GKM,removeGroupMemberRequest.build().toByteArray(),callback);
    }


    public void quitGroup(String groupId, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
        log.i("quitGroup "+groupId+" hasnotifyMsg "+(notifyMsg != null));
        WFCMessage.QuitGroupRequest.Builder builder = WFCMessage.QuitGroupRequest.newBuilder();
        builder.setGroupId(groupId);
        sendMessage(Signal.PUBLISH,SubSignal.GQ,builder.build().toByteArray(),callback);
    }

    public void modifyGroupInfo(String groupId, int modifyType, String newValue, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
        log.i("groupId "+groupId+" modifyType "+modifyType+" new value "+newValue);
        WFCMessage.ModifyGroupInfoRequest.Builder modifyGroupInfoBuilder = WFCMessage.ModifyGroupInfoRequest.newBuilder();
        modifyGroupInfoBuilder.setGroupId(groupId);
        modifyGroupInfoBuilder.setType(modifyType);
        modifyGroupInfoBuilder.setValue(newValue);
        sendMessage(Signal.PUBLISH,SubSignal.GMI,modifyGroupInfoBuilder.build().toByteArray(),callback);
    }

}
