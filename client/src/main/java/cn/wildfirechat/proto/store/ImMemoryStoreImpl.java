package cn.wildfirechat.proto.store;

import android.text.TextUtils;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUnreadCount;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.ProtoConstants;

public class ImMemoryStoreImpl implements ImMemoryStore{
    Log logger = LoggerFactory.getLogger(ImMemoryStoreImpl.class);
    private List<String> friendList = Collections.synchronizedList(new ArrayList<>());
    private Map<String,List<ProtoMessage>> protoMessageMap = new ConcurrentHashMap<>();
    private Map<String,Integer> unReadCountMap = new ConcurrentHashMap<>();
    private Map<String,Long> unReadMessageIdMap = new ConcurrentHashMap<>();
    private Map<String,ProtoConversationInfo> privateConversations = new ConcurrentHashMap<>();
    private Map<String,ProtoConversationInfo> groupConversations = new ConcurrentHashMap<>();
    private Map<String,ProtoGroupInfo> groupInfoMap = new ConcurrentHashMap<>();
    private Map<String,List<ProtoGroupMember>> groupMembersMap = new ConcurrentHashMap<>();
    private Map<String,ProtoUserInfo> userInfoMap = new ConcurrentHashMap<>();
    private List<ProtoFriendRequest> protoFriendRequestList = new ArrayList<>();
    private AtomicLong lastMessageSeq = new AtomicLong(0);
    private long friendRequestHead = 0;
    @Override
    public List<String> getFriendList() {
        return friendList;
    }

    @Override
    public String[] getFriendListArr() {
        String[] friendArr = new String[friendList.size()];
        friendList.toArray(friendArr);
        return friendArr;
    }

    @Override
    public synchronized void setFriendArr(String[] friendArr,boolean refresh) {
        if(friendArr != null){
            if(refresh){
                friendList.clear();
            }
            for(String friend : friendArr){
                if(!friendList.contains(friend)){
                    friendList.add(friend);
                }
            }
        }
    }

    @Override
    public void setFriendArr(String[] friendArr) {
        setFriendArr(friendArr,false);
    }


    @Override
    public boolean hasFriend() {
        return friendList.size() > 0;
    }

    @Override
    public boolean isMyFriend(String userId) {
        return friendList.contains(userId);
    }

    @Override
    public void addProtoMessageByTarget(String target, ProtoMessage protoMessage, boolean isPush) {
        logger.i("target "+target+" conversationtype "+protoMessage.getConversationType()+" add protomessage isPush "+isPush);
//        if(protoMessage.getContent().getType() >= 400){
//            logger.i("message content type "+protoMessage.getContent().getType()+" dont persistent");
//            return;
//        }
        if(protoMessage.getContent().getType() != MessageContentType.ContentType_Typing){
            if((!TextUtils.isEmpty(protoMessage.getContent().getPushContent())
                    || !TextUtils.isEmpty(protoMessage.getContent().getSearchableContent()))
                    || protoMessage.getContent().getBinaryContent() != null){

                //接收到的推送消息
                List<ProtoMessage> protoMessages = protoMessageMap.get(target);
                if(protoMessages != null){
                    protoMessages.add(protoMessage);
                } else {
                    protoMessages = new CopyOnWriteArrayList<>();
                    protoMessages.add(protoMessage);
                }

                protoMessageMap.put(target,protoMessages);

                if(isPush){
                    //设置未读消息
                    int unReadCount = 0;
                    if(unReadCountMap.get(protoMessage.getTarget()) != null){
                        unReadCount = unReadCountMap.get(protoMessage.getTarget());
                    }
                    unReadCountMap.put(protoMessage.getTarget(),++unReadCount);

                    //最后一次已读消息id
                    unReadMessageIdMap.put(target,protoMessage.getMessageId() - 1);
                }
                if(protoMessage.getConversationType() == ProtoConstants.ConversationType.ConversationType_Group){
                    logger.i("createGroupConversation "+protoMessage.getTarget());
                    createGroupConversation(protoMessage.getTarget());
                }
            }
        }

    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target) {
        List<ProtoMessage> protoMessages = protoMessageMap.get(target);
        if(protoMessages != null){
            ProtoMessage[] protoMessagesArr = new ProtoMessage[protoMessages.size()];
            protoMessages.toArray(protoMessagesArr);
            return filterProMessage(protoMessagesArr);
        }

        return new ProtoMessage[0];
    }

    @Override
    public ProtoMessage[] filterProMessage(ProtoMessage[] protoMessages){
        List<ProtoMessage> destPro = new ArrayList<>();
        for(ProtoMessage sourceProto : protoMessages){
            if(sourceProto.getContent().getType() < 400){
                destPro.add(sourceProto);
            }
        }
        ProtoMessage[] resultprotoMessage = new ProtoMessage[destPro.size()];
        return destPro.toArray(resultprotoMessage);
    }

    @Override
    public ProtoMessage getMessage(long messageId) {
        for(Map.Entry<String,List<ProtoMessage>> msgEntry: protoMessageMap.entrySet()){
            List<ProtoMessage> protoMessages = msgEntry.getValue();
            for(ProtoMessage protoMessage : protoMessages){
                if(protoMessage.getMessageId() == messageId){
                    logger.i("get messageId "+messageId);
                    return protoMessage;
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateMessageContent(ProtoMessage msg) {
        String target = msg.getTarget();
        if(TextUtils.isEmpty(target)){
            return false;
        } else {
            ProtoMessage removeMessage = null;
            List<ProtoMessage> protoMessages = protoMessageMap.get(target);
            for(ProtoMessage protoMessage : protoMessages){
                if(msg.getMessageId() == protoMessage.getMessageId()){
                    removeMessage = protoMessage;
                    break;
                }
            }
            if(removeMessage != null){
                protoMessages.remove(removeMessage);
                protoMessages.add(msg);
                return true;
            }
            return false;
        }
    }

    @Override
    public ProtoMessage getLastMessage(String target) {
        List<ProtoMessage> protoMessages = protoMessageMap.get(target);
        if(protoMessages != null){
            return protoMessages.get(protoMessages.size() -1);
        }
        return null;
    }

    @Override
    public long getTargetLastMessageId(String targetId) {
        if(TextUtils.isEmpty(targetId)){
            return 0;
        }
        return unReadMessageIdMap.get(targetId);
    }

    @Override
    public long getLastMessageSeq() {
        return lastMessageSeq.get();
    }

    @Override
    public void updateMessageSeq(long messageSeq) {
        lastMessageSeq.set(messageSeq);
    }

    @Override
    public void increaseMessageSeq() {
         lastMessageSeq.incrementAndGet();
    }

    @Override
    public void clearUnreadStatus(int conversationType, String target, int line) {
        unReadCountMap.put(target,0);
    }

    @Override
    public int getUnreadCount(String target) {
        return unReadCountMap.get(target) == null ? 0 : unReadCountMap.get(target);
    }

    @Override
    public void createPrivateConversation(String target) {
        ProtoConversationInfo protoConversationInfo = new ProtoConversationInfo();
        protoConversationInfo.setConversationType(Conversation.ConversationType.Single.ordinal());
        protoConversationInfo.setLine(0);
        protoConversationInfo.setTarget(target);
        ProtoMessage protoMessage = getLastMessage(target);
        if(protoMessage != null &&(!TextUtils.isEmpty(protoMessage.getContent().getPushContent())
                || !TextUtils.isEmpty(protoMessage.getContent().getSearchableContent())) ){
            protoMessage.setStatus(5);
        }
        protoConversationInfo.setLastMessage(protoMessage);
        ProtoUnreadCount protoUnreadCount = new ProtoUnreadCount();
        protoUnreadCount.setUnread(getUnreadCount(target));
        protoConversationInfo.setUnreadCount(protoUnreadCount);
        protoConversationInfo.setTimestamp(System.currentTimeMillis());
        privateConversations.put(target,protoConversationInfo);
    }

    @Override
    public List<ProtoConversationInfo> getPrivateConversations() {
        for(String friend : getFriendList()){
            //用户从friendlist列表进入创建会话可能会引起崩溃
            //if(getLastMessage(friend) != null){
                createPrivateConversation(friend);
            //}
        }
        List<ProtoConversationInfo> protoConversationInfoList = new ArrayList<>();
        if(privateConversations != null){
            for(Map.Entry<String,ProtoConversationInfo> entry : privateConversations.entrySet()){
                protoConversationInfoList.add(entry.getValue());
            }
        }
        return protoConversationInfoList;
    }

    @Override
    public void createGroupConversation(String groupId) {
        ProtoConversationInfo protoConversationInfo = new ProtoConversationInfo();
        protoConversationInfo.setConversationType(Conversation.ConversationType.Group.ordinal());
        protoConversationInfo.setLine(0);
        protoConversationInfo.setTarget(groupId);
        ProtoMessage protoMessage = getLastMessage(groupId);
        if(protoMessage != null &&(!TextUtils.isEmpty(protoMessage.getContent().getPushContent())
                || !TextUtils.isEmpty(protoMessage.getContent().getSearchableContent())) ){
            protoMessage.setStatus(5);
        }
        protoConversationInfo.setLastMessage(protoMessage);
        ProtoUnreadCount protoUnreadCount = new ProtoUnreadCount();
        logger.i("group "+groupId+" unread "+getUnreadCount(groupId));
        protoUnreadCount.setUnread(getUnreadCount(groupId));
        protoConversationInfo.setUnreadCount(protoUnreadCount);
        protoConversationInfo.setTimestamp(System.currentTimeMillis());
        groupConversations.put(groupId,protoConversationInfo);
    }

    @Override
    public List<ProtoConversationInfo> getGroupConversations() {
        List<ProtoConversationInfo> protoConversationInfoList = new ArrayList<>();
        if(groupConversations != null){
            for(Map.Entry<String,ProtoConversationInfo> entry : groupConversations.entrySet()){
                ProtoConversationInfo groupConversation = entry.getValue();ProtoUnreadCount protoUnreadCount = new ProtoUnreadCount();
                protoUnreadCount.setUnread(getUnreadCount(groupConversation.getTarget()));
                groupConversation.setUnreadCount(protoUnreadCount);
                protoConversationInfoList.add(groupConversation);
            }
        }
        return protoConversationInfoList;
    }

    @Override
    public ProtoConversationInfo getConversation(int conversationType, String target, int line) {
        logger.i("getConversation conversationType "+conversationType + " target "+target +" line "+line);
        if(conversationType == ProtoConstants.ConversationType.ConversationType_Private){
            return privateConversations.get(target);
        } else if(conversationType == ProtoConstants.ConversationType.ConversationType_Group){
            return groupConversations.get(target);
        }
        return new ProtoConversationInfo();
    }

    @Override
    public ProtoGroupInfo getGroupInfo(String groupId) {
        return groupInfoMap.get(groupId);
    }

    @Override
    public void addGroupInfo(String groupId, ProtoGroupInfo protoGroupInfo, boolean refresh) {
        if(protoGroupInfo != null){
            logger.i("add group "+groupId+" name->"+protoGroupInfo.getName()+" owner->"+protoGroupInfo.getOwner());
            groupInfoMap.put(groupId,protoGroupInfo);
        }
    }

    @Override
    public ProtoGroupMember[] getGroupMembers(String groupId) {
        List<ProtoGroupMember> groupMembers = groupMembersMap.get(groupId);
        if(groupMembers != null){
            ProtoGroupMember[] protoGroupMembers = new ProtoGroupMember[groupMembers.size()];
            groupMembers.toArray(protoGroupMembers);
            logger.i("getGroupMembers groupId "+groupId+" size "+protoGroupMembers.length);
            return protoGroupMembers;
        }
        return new ProtoGroupMember[0];
    }

    @Override
    public void addGroupMember(String groupId, ProtoGroupMember protoGroupMember) {
        List<ProtoGroupMember> groupMembers = groupMembersMap.get(groupId);
        if(groupMembers == null){
            groupMembers = new ArrayList<>();
        }

        for(ProtoGroupMember member : groupMembers){
            if(member.getMemberId().equals(protoGroupMember.getMemberId())){
                logger.i("member "+member.getMemberId()+" already add group"+groupId);
                return;
            }
        }
        groupMembers.add(protoGroupMember);
        groupMembersMap.put(groupId,groupMembers);
    }

    @Override
    public void addGroupMember(String groupId, ProtoGroupMember[] protoGroupMembers) {
        logger.i("add addGroupMember "+protoGroupMembers.length);
       if(protoGroupMembers != null){
           List<ProtoGroupMember> protoGroupMemberList = new ArrayList<>();
           for(ProtoGroupMember protoGroupMember : protoGroupMembers){
               protoGroupMemberList.add(protoGroupMember);
           }
           groupMembersMap.put(groupId,protoGroupMemberList);
       }
    }

    @Override
    public ProtoGroupMember getGroupMember(String groupId, String memberId) {
        List<ProtoGroupMember> groupMembers = groupMembersMap.get(groupId);
        if(groupMembers != null){
            for(ProtoGroupMember protoGroupMember : groupMembers){
                if(protoGroupMember.getMemberId().equals(memberId)){
                    return protoGroupMember;
                }
            }
        }
        return null;
    }

    @Override
    public ProtoUserInfo getUserInfo(String userId) {
        return userInfoMap.get(userId);
    }

    @Override
    public ProtoUserInfo[] getUserInfos(String[] userIds) {
        List<ProtoUserInfo> protoUserInfoList = new ArrayList<>();
        if(userIds != null){
            for(String useId : userIds){
                ProtoUserInfo protoUserInfo = userInfoMap.get(useId);
                if(protoUserInfo != null){
                    protoUserInfoList.add(protoUserInfo);
                }
            }
            ProtoUserInfo[] protoUserInfos = new ProtoUserInfo[protoUserInfoList.size()];
            protoUserInfoList.toArray(protoUserInfos);
            return protoUserInfos;
        }
        return null;
    }

    @Override
    public void addUserInfo(ProtoUserInfo protoUserInfos) {
        if(protoUserInfos != null){
            userInfoMap.put(protoUserInfos.getUid(),protoUserInfos);
        }
    }

    @Override
    public long getFriendRequestHead() {
        return friendRequestHead;
    }

    @Override
    public void setFriendRequestHead(long friendRequestHead) {
        logger.i("current friendHead is "+friendRequestHead);
        this.friendRequestHead = friendRequestHead;
    }

    @Override
    public ProtoFriendRequest[] getIncomingFriendRequest() {
        ProtoFriendRequest[] protoFriendRequests = new ProtoFriendRequest[protoFriendRequestList.size()];
        return protoFriendRequestList.toArray(protoFriendRequests);
    }

    @Override
    public void clearProtoFriendRequest() {
        protoFriendRequestList.clear();
    }

    @Override
    public void addProtoFriendRequest(ProtoFriendRequest protoFriendRequest) {
        for(ProtoFriendRequest friendRequest : protoFriendRequestList){
            if(friendRequest.getTarget().equals(protoFriendRequest.getTarget())){
                friendRequest.setStatus(protoFriendRequest.getStatus());
                return;
            }
        }
        protoFriendRequestList.add(protoFriendRequest);
    }

    @Override
    public void stop() {
        friendRequestHead = 0;
        protoFriendRequestList.clear();
        lastMessageSeq.set(0);
        friendList.clear();
        protoMessageMap.clear();
        unReadCountMap.clear();
        unReadMessageIdMap.clear();
        privateConversations.clear();
        groupConversations.clear();
        groupInfoMap.clear();
        groupMembersMap.clear();
        userInfoMap.clear();
    }
}
