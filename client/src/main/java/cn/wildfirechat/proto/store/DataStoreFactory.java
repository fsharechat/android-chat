package cn.wildfirechat.proto.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.List;
import java.util.Map;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUserInfo;

public class DataStoreFactory implements ImMemoryStore{

    private ImMemoryStore memoryStore;
    private ProtoMessageDataStore protoMessageDataStore;
    private static DataStoreFactory dataStoreFactory;
    private SharedPreferences preferences;

    private static final String LAST_MESSAGE_SEQ = "last_message_seq";

    public static ImMemoryStore getDataStore(Context context){
        if(dataStoreFactory == null){
            dataStoreFactory = new DataStoreFactory(context);
        }
        return dataStoreFactory;
    }

    private DataStoreFactory(Context context){
        memoryStore = new ImMemoryStoreImpl();
        protoMessageDataStore = new ProtoMessageDataStore(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public List<String> getFriendList() {
        return memoryStore.getFriendList();
    }

    @Override
    public String[] getFriendListArr() {
        return memoryStore.getFriendListArr();
    }

    @Override
    public void setFriendArr(String[] friendArr, boolean refresh) {
        memoryStore.setFriendArr(friendArr,refresh);
    }

    @Override
    public void setFriendArr(String[] friendArr) {
        memoryStore.setFriendArr(friendArr);
    }

    @Override
    public boolean hasFriend() {
        return memoryStore.hasFriend();
    }

    @Override
    public boolean isMyFriend(String userId) {
        return memoryStore.isMyFriend(userId);
    }

    @Override
    public long getFriendRequestHead() {
        return memoryStore.getFriendRequestHead();
    }

    @Override
    public void setFriendRequestHead(long friendRequestHead) {
        memoryStore.setFriendRequestHead(friendRequestHead);
    }

    @Override
    public ProtoFriendRequest[] getIncomingFriendRequest() {
        return memoryStore.getIncomingFriendRequest();
    }

    @Override
    public void clearProtoFriendRequest() {
        memoryStore.clearProtoFriendRequest();
    }

    @Override
    public void addProtoFriendRequest(ProtoFriendRequest protoFriendRequest) {
        memoryStore.addProtoFriendRequest(protoFriendRequest);
    }

    @Override
    public void addProtoMessageByTarget(String target, ProtoMessage protoMessage, boolean isPush) {
        memoryStore.addProtoMessageByTarget(target,protoMessage,isPush);
        if(protoMessage.getContent().getType() != MessageContentType.ContentType_Typing){
            if((!TextUtils.isEmpty(protoMessage.getContent().getPushContent())
                    || !TextUtils.isEmpty(protoMessage.getContent().getSearchableContent()))
                    || protoMessage.getContent().getBinaryContent() != null){
                protoMessageDataStore.addProtoMessageByTarget(target,protoMessage,isPush);
            }
        }
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target) {
        return getMessages(conversationType,target,0,0,false,20,null);
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser) {

        return new ProtoMessage[0];
    }

    @Override
    public ProtoMessage getMessage(long messageId) {
        ProtoMessage protoMessage = memoryStore.getMessage(messageId);
        if(protoMessage == null){
            protoMessage = protoMessageDataStore.getMessage(messageId);
        }
        return protoMessage;
    }

    @Override
    public ProtoMessage getMessageByUid(long messageUid) {
        ProtoMessage protoMessage = memoryStore.getMessageByUid(messageUid);
        if(protoMessage == null){
            protoMessage = protoMessageDataStore.getMessageByUid(messageUid);
        }
        return protoMessage;
    }

    @Override
    public boolean deleteMessage(long messageId) {
        protoMessageDataStore.deleteMessage(messageId);
        return memoryStore.deleteMessage(messageId);
    }

    @Override
    public ProtoMessage[] filterProMessage(ProtoMessage[] protoMessages) {
        return memoryStore.filterProMessage(protoMessages);
    }

    @Override
    public boolean updateMessageContent(ProtoMessage msg) {
        protoMessageDataStore.updateMessageContent(msg);
        return memoryStore.updateMessageContent(msg);
    }

    @Override
    public boolean updateMessageStatus(long protoMessageId, int status) {
        protoMessageDataStore.updateMessageStatus(protoMessageId,status);
        return memoryStore.updateMessageStatus(protoMessageId,status);
    }

    @Override
    public boolean updateMessageUid(long protoMessageId, long messageUid) {
        protoMessageDataStore.updateMessageUid(protoMessageId,messageUid);
        return memoryStore.updateMessageUid(protoMessageId,messageUid);
    }

    @Override
    public ProtoMessage getLastMessage(String target) {
        ProtoMessage protoMessage = memoryStore.getLastMessage(target);
        if(protoMessage == null){
            protoMessage = protoMessageDataStore.getLastMessage(target);
        }
        return protoMessage;
    }

    @Override
    public long getTargetLastMessageId(String targetId) {
        return memoryStore.getTargetLastMessageId(targetId);
    }

    @Override
    public long getLastMessageSeq() {
        long lastMessageSeq = memoryStore.getLastMessageSeq();
        if(lastMessageSeq == 0){
            lastMessageSeq = preferences.getLong(LAST_MESSAGE_SEQ,0);
        }
        return lastMessageSeq;
    }

    @Override
    public void updateMessageSeq(long messageSeq) {
        memoryStore.updateMessageSeq(messageSeq);
        preferences.edit().putLong(LAST_MESSAGE_SEQ,messageSeq).apply();
    }

    @Override
    public long increaseMessageSeq() {
        long messageSeq = memoryStore.increaseMessageSeq();
        preferences.edit().putLong(LAST_MESSAGE_SEQ,messageSeq).apply();
        return messageSeq;
    }

    @Override
    public void clearUnreadStatus(int conversationType, String target, int line) {
        memoryStore.clearUnreadStatus(conversationType,target,line);
    }

    @Override
    public int getUnreadCount(String target) {
        return memoryStore.getUnreadCount(target);
    }

    @Override
    public void createPrivateConversation(String target) {
        memoryStore.createPrivateConversation(target);
    }

    @Override
    public List<ProtoConversationInfo> getPrivateConversations() {
        return memoryStore.getPrivateConversations();
    }

    @Override
    public void createGroupConversation(String groupId) {
        memoryStore.createGroupConversation(groupId);
    }

    @Override
    public List<ProtoConversationInfo> getGroupConversations() {
        return memoryStore.getGroupConversations();
    }

    @Override
    public ProtoConversationInfo getConversation(int conversationType, String target, int line) {
        return memoryStore.getConversation(conversationType,target,line);
    }

    @Override
    public ProtoGroupInfo getGroupInfo(String groupId) {
        return memoryStore.getGroupInfo(groupId);
    }

    @Override
    public void addGroupInfo(String groupId, ProtoGroupInfo protoGroupInfo, boolean refresh) {
        memoryStore.addGroupInfo(groupId,protoGroupInfo,refresh);
    }

    @Override
    public ProtoGroupMember[] getGroupMembers(String groupId) {
        return memoryStore.getGroupMembers(groupId);
    }

    @Override
    public void addGroupMember(String groupId, ProtoGroupMember protoGroupMember) {
        memoryStore.addGroupMember(groupId,protoGroupMember);
    }

    @Override
    public void addGroupMember(String groupId, ProtoGroupMember[] protoGroupMembers) {
        memoryStore.addGroupMember(groupId,protoGroupMembers);
    }

    @Override
    public ProtoGroupMember getGroupMember(String groupId, String memberId) {
        return memoryStore.getGroupMember(groupId,memberId);
    }

    @Override
    public ProtoUserInfo getUserInfo(String userId) {
        return memoryStore.getUserInfo(userId);
    }

    @Override
    public ProtoUserInfo[] getUserInfos(String[] userIds) {
        return memoryStore.getUserInfos(userIds);
    }

    @Override
    public void addUserInfo(ProtoUserInfo protoUserInfos) {
        memoryStore.addUserInfo(protoUserInfos);
    }

    @Override
    public void setUserSetting(int scope, String key, String value) {
         memoryStore.setUserSetting(scope,key,value);
    }

    @Override
    public String getUserSetting(int scope, String key) {
        return memoryStore.getUserSetting(scope,key);
    }

    @Override
    public Map<String, String> getUserSettings(int scope) {
        return memoryStore.getUserSettings(scope);
    }

    @Override
    public void stop() {
        memoryStore.stop();
    }
}
