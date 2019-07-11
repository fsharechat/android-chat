package cn.wildfirechat.proto.store;


import java.util.List;
import java.util.Map;

import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUserInfo;

public interface ImMemoryStore {
    List<String> getFriendList();
    String[] getFriendListArr();
    void setFriendArr(String[] friendArr,boolean refresh);
    void setFriendArr(String[] friendArr);
    boolean hasFriend();
    boolean isMyFriend(String userId);
    long getFriendRequestHead();
    void setFriendRequestHead(long friendRequestHead);
    ProtoFriendRequest[] getIncomingFriendRequest();
    void clearProtoFriendRequest();
    void addProtoFriendRequest(ProtoFriendRequest protoFriendRequest);
    void addProtoMessageByTarget(String target, ProtoMessage protoMessage, boolean isPush);
    void addProtoMessagesByTarget(String target, List<ProtoMessage> protoMessages, boolean isPush);
    ProtoMessage[] getMessages(int conversationType, String target);
    ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser);
    ProtoMessage getMessage(long messageId);
    ProtoMessage getMessageByUid(long messageUid);
    boolean deleteMessage(long messageId);
    ProtoMessage[] filterProMessage(ProtoMessage[] protoMessages);
    boolean updateMessageContent(ProtoMessage msg);
    boolean updateMessageStatus(long protoMessageId,int status);
    boolean updateMessageUid(long protoMessageId,long messageUid);
    ProtoMessage getLastMessage(String target);
    long getTargetLastMessageId(String targetId);
    long getLastMessageSeq();
    void updateMessageSeq(long messageSeq);
    long increaseMessageSeq();
    void clearUnreadStatus(int conversationType, String target, int line);
    int getUnreadCount(String target);
    void createPrivateConversation(String target);
    void removeConversation(int conversationType, String target, int line, boolean clearMsg);
    void setConversationDraft(int conversationType, String target, int line, String draft);
    List<ProtoConversationInfo> getPrivateConversations();
    void createGroupConversation(String groupId);
    List<ProtoConversationInfo> getGroupConversations();
    ProtoConversationInfo getConversation(int conversationType, String target, int line);
    ProtoConversationInfo[] getConversations(int[] conversationTypes, int[] lines);
    ProtoGroupInfo getGroupInfo(String groupId);
    void addGroupInfo(String groupId,ProtoGroupInfo protoGroupInfo,boolean refresh);
    ProtoGroupMember[] getGroupMembers(String groupId);
    void addGroupMember(String groupId,ProtoGroupMember protoGroupMember);
    void addGroupMember(String groupId,ProtoGroupMember[] protoGroupMembers);
    ProtoGroupMember getGroupMember(String groupId, String memberId);
    ProtoUserInfo getUserInfo(String userId);
    ProtoUserInfo[] getUserInfos(String[] userIds);
    void addUserInfo(ProtoUserInfo protoUserInfos);
    void setUserSetting(int scope, String key, String value);
    String getUserSetting(int scope, String key);
    Map<String, String> getUserSettings(int scope);
    void stop();
}
