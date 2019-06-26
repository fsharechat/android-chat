package cn.wildfirechat.proto.store;


import java.util.List;

import cn.wildfirechat.model.ProtoConversationInfo;
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
    void addProtoMessageByTarget(String target, ProtoMessage protoMessage, boolean isPush);
    ProtoMessage[] getMessages(int conversationType, String target);
    ProtoMessage getLastMessage(String target);
    long getTargetLastMessageId(String targetId);
    long getLastMessageSeq();
    void updateMessageSeq(long messageSeq);
    void clearUnreadStatus(int conversationType, String target, int line);
    int getUnreadCount(String target);
    void createPrivateConversation(String target);
    List<ProtoConversationInfo> getPrivateConversations();
    void createGroupConversation(String groupId);
    List<ProtoConversationInfo> getGroupConversations();
    ProtoConversationInfo getConversation(int conversationType, String target, int line);
    ProtoGroupInfo getGroupInfo(String groupId);
    void addGroupInfo(String groupId,ProtoGroupInfo protoGroupInfo,boolean refresh);
    ProtoGroupMember[] getGroupMembers(String groupId);
    void addGroupMember(String groupId,ProtoGroupMember protoGroupMember);
    void addGroupMember(String groupId,ProtoGroupMember[] protoGroupMembers);
    ProtoGroupMember getGroupMember(String groupId, String memberId);
    ProtoUserInfo getUserInfo(String userId);
    ProtoUserInfo[] getUserInfos(String[] userIds);
    void addUserInfo(ProtoUserInfo protoUserInfos);
    void stop();
}
