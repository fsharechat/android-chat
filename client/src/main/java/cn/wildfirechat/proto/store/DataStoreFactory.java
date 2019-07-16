package cn.wildfirechat.proto.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUserInfo;

public class DataStoreFactory implements ImMemoryStore{
    Log logger = LoggerFactory.getLogger(DataStoreFactory.class);
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
        protoMessageDataStore.addProtoMessageByTarget(target,protoMessage,isPush);
    }

    @Override
    public void addProtoMessagesByTarget(String target, List<ProtoMessage> protoMessages, boolean isPush) {
        protoMessageDataStore.addProtoMessagesByTarget(target,protoMessages,isPush);
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target) {
        return getMessages(conversationType,target,0,0,false,20,null);
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser) {
        return filterProMessage(protoMessageDataStore.getMessages(conversationType,target,line,fromIndex,before,count,withUser));
    }

    @Override
    public ProtoMessage getMessage(long messageId) {
        return protoMessageDataStore.getMessage(messageId);
    }

    @Override
    public ProtoMessage getMessageByUid(long messageUid) {
        return protoMessageDataStore.getMessageByUid(messageUid);
    }

    @Override
    public boolean deleteMessage(long messageId) {
        return protoMessageDataStore.deleteMessage(messageId);
    }

    @Override
    public ProtoMessage[] filterProMessage(ProtoMessage[] protoMessages) {
        List<ProtoMessage> destPro = new ArrayList<>();
        for(ProtoMessage sourceProto : protoMessages){
            if(canPersistent(sourceProto.getContent().getType())){
                destPro.add(sourceProto);
            }
        }
        ProtoMessage[] resultprotoMessage = new ProtoMessage[destPro.size()];
        return destPro.toArray(resultprotoMessage);
    }

    @Override
    public boolean updateMessageContent(ProtoMessage msg) {
        return protoMessageDataStore.updateMessageContent(msg);
    }

    @Override
    public boolean updateMessageStatus(long protoMessageId, int status) {
        return protoMessageDataStore.updateMessageStatus(protoMessageId,status);
    }

    @Override
    public boolean updateMessageUid(long protoMessageId, long messageUid) {
        return protoMessageDataStore.updateMessageUid(protoMessageId,messageUid);
    }

    @Override
    public boolean canPersistent(int contentType) {
        return protoMessageDataStore.canPersistent(contentType);
    }

    @Override
    public ProtoMessage getLastMessage(String target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTargetLastMessageId(String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized long getLastMessageSeq() {
        long lastSeq = preferences.getLong(LAST_MESSAGE_SEQ,0);
        logger.i("getLastMessageSeq "+lastSeq);
        return lastSeq;
    }

    @Override
    public synchronized void updateMessageSeq(long messageSeq) {
        preferences.edit().putLong(LAST_MESSAGE_SEQ,messageSeq).commit();
        logger.i("updateMessageSeq "+messageSeq);
    }

    @Override
    public synchronized long increaseMessageSeq() {
        long seq = preferences.getLong(LAST_MESSAGE_SEQ,0);
        preferences.edit().putLong(LAST_MESSAGE_SEQ,++seq).commit();
        logger.i("increaseMessageSeq "+seq);
        return seq;
    }

    @Override
    public void clearUnreadStatus(int conversationType, String target, int line) {
        protoMessageDataStore.clearUnreadStatus(conversationType,target,line);
    }

    @Override
    public int getUnreadCount(String target) {
        return protoMessageDataStore.getUnreadCount(target);
    }

    @Override
    public void createPrivateConversation(String target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeConversation(int conversationType, String target, int line, boolean clearMsg) {
        protoMessageDataStore.removeConversation(conversationType,target,line,clearMsg);
    }

    @Override
    public void setConversationDraft(int conversationType, String target, int line, String draft) {
        protoMessageDataStore.setConversationDraft(conversationType,target,line,draft);
    }

    @Override
    public List<ProtoConversationInfo> getPrivateConversations() {
        return protoMessageDataStore.getPrivateConversations();
    }

    @Override
    public void createGroupConversation(String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProtoConversationInfo> getGroupConversations() {
        return protoMessageDataStore.getGroupConversations();
    }

    @Override
    public ProtoConversationInfo getConversation(int conversationType, String target, int line) {
        return protoMessageDataStore.getConversation(conversationType,target,line);
    }

    @Override
    public ProtoConversationInfo[] getConversations(int[] conversationTypes, int[] lines) {
        return new ProtoConversationInfo[0];
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
         protoMessageDataStore.setUserSetting(scope,key,value);
    }

    @Override
    public String getUserSetting(int scope, String key) {
        return protoMessageDataStore.getUserSetting(scope,key);
    }

    @Override
    public Map<String, String> getUserSettings(int scope) {
        return protoMessageDataStore.getUserSettings(scope);
    }

    @Override
    public void stop() {
        preferences.edit().putLong(LAST_MESSAGE_SEQ,0).apply();
        memoryStore.stop();
        protoMessageDataStore.stop();
    }
}
