package cn.wildfirechat.proto.store;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoConversationSearchresult;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoGroupSearchResult;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUserInfo;

public class DataStoreAdapter implements ImMemoryStore{
    @Override
    public List<String> getFriendList() {
        return null;
    }

    @Override
    public String[] getFriendListArr() {
        return new String[0];
    }

    @Override
    public void setFriendArr(String[] friendArr, boolean refresh) {

    }

    @Override
    public void setFriendArr(String[] friendArr) {

    }

    @Override
    public boolean hasFriend() {
        return false;
    }

    @Override
    public boolean isMyFriend(String userId) {
        return false;
    }

    @Override
    public ProtoUserInfo[] searchFriends(String keyword) {
        return new ProtoUserInfo[0];
    }

    @Override
    public long getFriendRequestHead() {
        return 0;
    }

    @Override
    public void setFriendRequestHead(long friendRequestHead) {

    }

    @Override
    public ProtoFriendRequest[] getIncomingFriendRequest() {
        return new ProtoFriendRequest[0];
    }

    @Override
    public void clearProtoFriendRequest() {

    }

    @Override
    public void addProtoFriendRequest(ProtoFriendRequest protoFriendRequest) {

    }

    @Override
    public void addProtoMessageByTarget(String target, ProtoMessage protoMessage, boolean isPush) {

    }

    @Override
    public void addProtoMessagesByTarget(String target, List<ProtoMessage> protoMessages, boolean isPush) {

    }

    @Override
    public ProtoMessage[] searchMessage(int conversationType, String target, int line, String keyword) {
        return new ProtoMessage[0];
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target) {
        return new ProtoMessage[0];
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser) {
        return new ProtoMessage[0];
    }

    @Override
    public ProtoMessage getMessage(long messageId) {
        return null;
    }

    @Override
    public ProtoMessage getMessageByUid(long messageUid) {
        return null;
    }

    @Override
    public boolean deleteMessage(long messageId) {
        return false;
    }

    @Override
    public ProtoMessage[] filterProMessage(ProtoMessage[] protoMessages) {
        return new ProtoMessage[0];
    }

    @Override
    public boolean updateMessageContent(ProtoMessage msg) {
        return false;
    }

    @Override
    public boolean updateMessageStatus(long protoMessageId, int status) {
        return false;
    }

    @Override
    public boolean updateMessageUid(long protoMessageId, long messageUid) {
        return false;
    }

    @Override
    public boolean canPersistent(int contentType) {
        return false;
    }

    @Override
    public ProtoMessage getLastMessage(String target) {
        return null;
    }

    @Override
    public long getTargetLastMessageId(String targetId) {
        return 0;
    }

    @Override
    public long getLastMessageSeq() {
        return 0;
    }

    @Override
    public void updateMessageSeq(long messageSeq) {

    }

    @Override
    public long increaseMessageSeq() {
        return 0;
    }

    @Override
    public void clearUnreadStatus(int conversationType, String target, int line) {

    }

    @Override
    public int getUnreadCount(String target) {
        return 0;
    }

    @Override
    public ProtoConversationSearchresult[] searchConversation(String keyword, int[] conversationTypes, int[] lines) {
        return new ProtoConversationSearchresult[0];
    }

    @Override
    public void createPrivateConversation(String target) {

    }

    @Override
    public void removeConversation(int conversationType, String target, int line, boolean clearMsg) {

    }

    @Override
    public void setConversationDraft(int conversationType, String target, int line, String draft) {

    }

    @Override
    public List<ProtoConversationInfo> getPrivateConversations() {
        return null;
    }

    @Override
    public void createGroupConversation(String groupId) {

    }

    @Override
    public List<ProtoConversationInfo> getGroupConversations() {
        return null;
    }

    @Override
    public ProtoConversationInfo getConversation(int conversationType, String target, int line) {
        return null;
    }

    @Override
    public ProtoConversationInfo[] getConversations(int[] conversationTypes, int[] lines) {
        return new ProtoConversationInfo[0];
    }

    @Override
    public ProtoGroupInfo getGroupInfo(String groupId) {
        return null;
    }

    @Override
    public void addGroupInfo(String groupId, ProtoGroupInfo protoGroupInfo, boolean refresh) {

    }

    @Override
    public ProtoGroupMember[] getGroupMembers(String groupId) {
        return new ProtoGroupMember[0];
    }

    @Override
    public void addGroupMember(String groupId, ProtoGroupMember protoGroupMember) {

    }

    @Override
    public void addGroupMember(String groupId, ProtoGroupMember[] protoGroupMembers) {

    }

    @Override
    public ProtoGroupSearchResult[] searchGroups(String keyword) {
        return new ProtoGroupSearchResult[0];
    }

    @Override
    public ProtoGroupMember getGroupMember(String groupId, String memberId) {
        return null;
    }

    @Override
    public ProtoUserInfo getUserInfo(String userId) {
        return null;
    }

    @Override
    public ProtoUserInfo[] getUserInfos(String[] userIds) {
        return new ProtoUserInfo[0];
    }

    @Override
    public void addUserInfo(ProtoUserInfo protoUserInfos) {

    }

    @Override
    public void setUserSetting(int scope, String key, String value) {

    }

    @Override
    public String getUserSetting(int scope, String key) {
        return null;
    }

    @Override
    public Map<String, String> getUserSettings(int scope) {
        return null;
    }

    @Override
    public void saveRequestProtoMessageIds(Set<String> protoMessageIds) {

    }

    @Override
    public Set<String> getRequestProtoMessageIds() {
        return null;
    }

    @Override
    public void stop() {

    }
}
