package cn.wildfirechat.proto.store;


import java.util.List;

import cn.wildfirechat.model.ProtoMessage;

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
    void clearUnreadStatus(int conversationType, String target, int line);
    int getUnreadCount(String target);
//    void addUnReadMessageByTarget(String target,ProtoMessage protoMessage);
}
