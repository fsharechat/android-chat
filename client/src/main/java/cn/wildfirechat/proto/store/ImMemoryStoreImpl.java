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

import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUnreadCount;
import cn.wildfirechat.proto.ProtoConstants;

public class ImMemoryStoreImpl implements ImMemoryStore{
    Log logger = LoggerFactory.getLogger(ImMemoryStoreImpl.class);
    private List<String> friendList = Collections.synchronizedList(new ArrayList<>());
    private Map<String,List<ProtoMessage>> protoMessageMap = new ConcurrentHashMap<>();
    private Map<String,Integer> unReadCountMap = new ConcurrentHashMap<>();
    private Map<String,Long> unReadMessageIdMap = new ConcurrentHashMap<>();
    private Map<String,ProtoConversationInfo> privateConversations = new ConcurrentHashMap<>();
    private Map<String,ProtoConversationInfo> groupConversations = new ConcurrentHashMap<>();
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
        if((!TextUtils.isEmpty(protoMessage.getContent().getPushContent()) || !TextUtils.isEmpty(protoMessage.getContent().getSearchableContent()))){
            //接收到的推送消息
            List<ProtoMessage> protoMessages = protoMessageMap.get(target);
            if(protoMessages != null){
                protoMessages.add(protoMessage);
            } else {
                protoMessages = new LinkedList<>();
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
                createGroupConversation(protoMessage.getTarget());
            }
        }
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target) {
        List<ProtoMessage> protoMessages = protoMessageMap.get(target);
        if(protoMessages != null){
            ProtoMessage[] protoMessagesArr = new ProtoMessage[protoMessages.size()];
            protoMessages.toArray(protoMessagesArr);
            return protoMessagesArr;
        }

        return new ProtoMessage[0];
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
    public ProtoConversationInfo[] getPrivateConversations() {
        for(String friend : getFriendList()){
            if(getLastMessage(friend) != null){
                createPrivateConversation(friend);
            }
        }
        if(privateConversations != null){
            ProtoConversationInfo[] protoConversationInfos = new ProtoConversationInfo[privateConversations.size()];
            List<ProtoConversationInfo> protoConversationInfoList = new ArrayList<>();
            for(Map.Entry<String,ProtoConversationInfo> entry : privateConversations.entrySet()){
                protoConversationInfoList.add(entry.getValue());
            }
            protoConversationInfoList.toArray(protoConversationInfos);
            return protoConversationInfos;
        }

        return new ProtoConversationInfo[0];
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
        protoUnreadCount.setUnread(getUnreadCount(groupId));
        protoConversationInfo.setUnreadCount(protoUnreadCount);
        protoConversationInfo.setTimestamp(System.currentTimeMillis());
        groupConversations.put(groupId,protoConversationInfo);
    }

    @Override
    public ProtoConversationInfo[] getGroupConversations() {
        if(groupConversations != null){
            ProtoConversationInfo[] protoConversationInfos = new ProtoConversationInfo[groupConversations.size()];
            List<ProtoConversationInfo> protoConversationInfoList = new ArrayList<>();
            for(Map.Entry<String,ProtoConversationInfo> entry : groupConversations.entrySet()){
                protoConversationInfoList.add(entry.getValue());
            }
            protoConversationInfoList.toArray(protoConversationInfos);
            return protoConversationInfos;
        }
        return new ProtoConversationInfo[0];
    }

    @Override
    public ProtoConversationInfo getConversation(int conversationType, String target, int line) {
        if(conversationType == ProtoConstants.ConversationType.ConversationType_Private){
            return privateConversations.get(target);
        } else if(conversationType == ProtoConstants.ConversationType.ConversationType_Group){
            return groupConversations.get(target);
        }
        return new ProtoConversationInfo();
    }
}
