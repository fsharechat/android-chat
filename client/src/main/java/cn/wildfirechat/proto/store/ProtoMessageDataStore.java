package cn.wildfirechat.proto.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUserInfo;

class ProtoMessageDataStore extends SqliteDatabaseStore{

    private long lastInsertedMessageRowId = -1;

    public static final String COLUMN_ID            = "id";
    public static final String COLUMN_MESSAGE_TARGET = "message_target";
    public static final String COLUMN_MESSAGE_DATA    = "message_data";
    public static final String COLUMN_MESSAGE_ID   = "message_id";
    public static final String COLUMN_MESSAGE_UID    = "message_uid";
    public static final String COLUMN_DATE_CREATED  = "date_created";

    private String[] allMessageColumns = {
            COLUMN_ID,
            COLUMN_MESSAGE_TARGET,
            COLUMN_MESSAGE_ID,
            COLUMN_MESSAGE_UID,
            COLUMN_MESSAGE_DATA,
            COLUMN_DATE_CREATED
    };

    public ProtoMessageDataStore(Context context){
        super(context);
    }

    private List<Map<String, Object>> queryMessageDatabase(String query, String orderBy) {
        List<Map<String, Object>> res = new ArrayList<>();
        if (isDatabaseOpen()) {
            Cursor cursor = database.query(ChatStoreHelper.TABLE_MESSAGES, allMessageColumns, query,
                    null, null, null, orderBy);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Map<String, Object> eventMetadata = new HashMap<>();
                eventMetadata.put(COLUMN_ID, cursor.getLong(0));
                eventMetadata.put(COLUMN_MESSAGE_ID, cursor.getLong(1));
                eventMetadata.put(COLUMN_MESSAGE_UID, cursor.getLong(2));
                eventMetadata.put(COLUMN_MESSAGE_DATA, deserializer(cursor.getBlob(3)));
                eventMetadata.put(COLUMN_DATE_CREATED, cursor.getString(4));
                cursor.moveToNext();
                res.add(eventMetadata);
            }
            cursor.close();
        }
        return res;
    }


    public void insertProtoMessage(String target ,ProtoMessage protoMessage){
        if (isDatabaseOpen()) {
            byte[] bytes = serialize(protoMessage);
            ContentValues values = new ContentValues(4);
            values.put(COLUMN_MESSAGE_DATA, bytes);
            values.put(COLUMN_MESSAGE_TARGET,target);
            values.put(COLUMN_MESSAGE_UID,protoMessage.getMessageUid());
            values.put(COLUMN_MESSAGE_ID,protoMessage.getMessageId());
            lastInsertedMessageRowId = database.insert(ChatStoreHelper.TABLE_MESSAGES, null, values);
        }
        logger.i("Added event to database: "+lastInsertedMessageRowId);
    }

    @Override
    public ProtoMessage getMessageByUid(long messageUid) {
        List<Map<String, Object>> res = queryMessageDatabase(COLUMN_MESSAGE_UID + "=" + messageUid, null);
        if (!res.isEmpty()) {
            Map<String,Object> protoMessageMap = res.get(0);
            ProtoMessage protoMessage = (ProtoMessage) protoMessageMap.get(COLUMN_MESSAGE_DATA);
            return protoMessage;
        }
        else {
            return null;
        }
    }

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
         insertProtoMessage(target,protoMessage);
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target) {
        return new ProtoMessage[0];
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser) {
        List<Map<String, Object>> messageList = queryMessageDatabase(COLUMN_MESSAGE_TARGET + "=" + target,"COLUMN_DATE_CREATED DESC LIMIT " + count+" OFFSET "+fromIndex);
        List<ProtoMessage> protoMessages = new ArrayList<>();
        for(Map<String,Object> objectMap : messageList){
            protoMessages.add((ProtoMessage) objectMap.get(COLUMN_MESSAGE_DATA));
        }
        ProtoMessage[] protoMessagesArr = new ProtoMessage[protoMessages.size()];
        return protoMessages.toArray(protoMessagesArr);
    }

    @Override
    public ProtoMessage getMessage(long messageId) {
        List<Map<String, Object>> res = queryMessageDatabase(COLUMN_MESSAGE_ID + "=" + messageId, null);
        if (!res.isEmpty()) {
            Map<String,Object> protoMessageMap = res.get(0);
            ProtoMessage protoMessage = (ProtoMessage) protoMessageMap.get(COLUMN_MESSAGE_DATA);
            return protoMessage;
        }
        else {
            return null;
        }
    }

    @Override
    public boolean deleteMessage(long messageId) {
        int retval = -1;
        if (isDatabaseOpen()) {
            retval = database.delete(ChatStoreHelper.TABLE_MESSAGES,
                    COLUMN_MESSAGE_ID + "=" + messageId, null);
        }
        logger.i("Removed event from database: "+ messageId);
        return retval == 1;
    }

    @Override
    public ProtoMessage[] filterProMessage(ProtoMessage[] protoMessages) {
        return new ProtoMessage[0];
    }

    @Override
    public boolean updateMessageContent(ProtoMessage msg) {
        int retval = -1;
        if(isDatabaseOpen()){
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(COLUMN_MESSAGE_DATA,serialize(msg));
            retval = database.update(ChatStoreHelper.TABLE_MESSAGES,contentValues,COLUMN_MESSAGE_ID + "=" + msg.getMessageId(),null);
            logger.i("update protomessageContent id "+msg.getMessageId());
            return retval == 1;
        }
        return false;
    }

    @Override
    public boolean updateMessageStatus(long protoMessageId, int status) {
        int retval = -1;
        if(isDatabaseOpen()){
            ProtoMessage updateProtoMessage = getMessage(protoMessageId);
            updateProtoMessage.setStatus(status);
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(COLUMN_MESSAGE_DATA,serialize(updateProtoMessage));
            retval = database.update(ChatStoreHelper.TABLE_MESSAGES,contentValues,COLUMN_MESSAGE_ID + "=" + protoMessageId,null);
            logger.i("update protomessage status id "+status);
            return retval == 1;
        }
        return false;
    }

    @Override
    public boolean updateMessageUid(long protoMessageId, long messageUid) {
        int retval = -1;
        if(isDatabaseOpen()){
            ProtoMessage updateProtoMessage = getMessage(protoMessageId);
            updateProtoMessage.setMessageUid(messageUid);
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(COLUMN_MESSAGE_DATA,serialize(updateProtoMessage));
            retval = database.update(ChatStoreHelper.TABLE_MESSAGES,contentValues,COLUMN_MESSAGE_ID + "=" + protoMessageId,null);
            logger.i("update protomessage uid id "+messageUid);
            return retval == 1;
        }
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
    public void createPrivateConversation(String target) {

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
    public void stop() {

    }
}
