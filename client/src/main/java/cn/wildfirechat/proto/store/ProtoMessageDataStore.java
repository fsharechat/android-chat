package cn.wildfirechat.proto.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoUnreadCount;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Accept;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Accept_T;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_End;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Modify;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Signal;
import static cn.wildfirechat.remote.UserSettingScope.ConversationSilent;
import static cn.wildfirechat.remote.UserSettingScope.ConversationTop;

class ProtoMessageDataStore extends SqliteDatabaseStore{

    private long lastInsertedMessageRowId = -1;
    private long lastInsertedConversationRowId = -1;
    private Map<String,Integer> unReadCountMap = new ConcurrentHashMap<>();
    private Map<Integer,Map<String,String>> userSettingMap = new ConcurrentHashMap<>();
    public static final String COLUMN_ID            = "id";
    public static final String COLUMN_MESSAGE_TARGET = "message_target";
    public static final String COLUMN_MESSAGE_DATA    = "message_data";
    public static final String COLUMN_MESSAGE_ID   = "message_id";
    public static final String COLUMN_MESSAGE_UID    = "message_uid";
    public static final String COLUMN_DATE_CREATED  = "date_created";


    public static final String COLUMN_CONVERSATION_TYPE = "conversation_type";
    public static final String COLUMN_CONVERSATION_TARGET = "conversation_target";
    public static final String COLUMN_CONVERSATION_LINE = "conversation_line";
    public static final String COLUMN_CONVERSATION_INFO = "conversation_info";

    private String[] allMessageColumns = {
            COLUMN_ID,
            COLUMN_MESSAGE_TARGET,
            COLUMN_MESSAGE_ID,
            COLUMN_MESSAGE_UID,
            COLUMN_MESSAGE_DATA,
            COLUMN_DATE_CREATED
    };


    private String[] allConversationsColumns = {
            COLUMN_ID,
            COLUMN_CONVERSATION_TYPE,
            COLUMN_CONVERSATION_TARGET,
            COLUMN_CONVERSATION_LINE,
            COLUMN_CONVERSATION_INFO,
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
                eventMetadata.put(COLUMN_MESSAGE_TARGET,cursor.getString(1));
                eventMetadata.put(COLUMN_MESSAGE_ID, cursor.getLong(2));
                eventMetadata.put(COLUMN_MESSAGE_UID, cursor.getLong(3));
                eventMetadata.put(COLUMN_MESSAGE_DATA, deserializer(cursor.getBlob(4)));
                eventMetadata.put(COLUMN_DATE_CREATED, cursor.getString(5));
                cursor.moveToNext();
                res.add(eventMetadata);
            }
            cursor.close();
        }
        return res;
    }

    private List<Map<String,Object>> queryConversation(String query,String orderBy){
        List<Map<String, Object>> res = new ArrayList<>();
        if (isDatabaseOpen()) {
            Cursor cursor = database.query(ChatStoreHelper.TABLE_CONVERSATIONS, allConversationsColumns, query,
                    null, null, null, orderBy);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Map<String, Object> eventMetadata = new HashMap<>();
                eventMetadata.put(COLUMN_ID, cursor.getLong(0));
                eventMetadata.put(COLUMN_CONVERSATION_TYPE,cursor.getInt(1));
                eventMetadata.put(COLUMN_CONVERSATION_TARGET, cursor.getString(2));
                eventMetadata.put(COLUMN_CONVERSATION_LINE,cursor.getInt(3));
                eventMetadata.put(COLUMN_CONVERSATION_INFO, deserializer(cursor.getBlob(4)));
                eventMetadata.put(COLUMN_DATE_CREATED, cursor.getString(5));
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
            logger.i("insert messageId  "+protoMessage.getMessageId()+" target "+protoMessage.getTarget()+" uid "+protoMessage.getMessageUid()+" content "+protoMessage.getContent().getSearchableContent());
            lastInsertedMessageRowId = database.insert(ChatStoreHelper.TABLE_MESSAGES, null, values);
        }
        logger.i("Added event to database: "+lastInsertedMessageRowId);
    }


    private void insertConversations(String target,int type, int line, ProtoConversationInfo protoConversationInfo){
        if (isDatabaseOpen()) {
            byte[] bytes = serialize(protoConversationInfo);
            ContentValues values = new ContentValues(4);
            values.put(COLUMN_CONVERSATION_INFO, bytes);
            values.put(COLUMN_CONVERSATION_TYPE,type);
            values.put(COLUMN_CONVERSATION_TARGET,target);
            values.put(COLUMN_CONVERSATION_LINE,line);
            lastInsertedConversationRowId = database.insert(ChatStoreHelper.TABLE_CONVERSATIONS, null, values);
        }
        logger.i("Added event to database: "+lastInsertedConversationRowId);
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
    public boolean canPersistent(int contentType) {
        return contentType != MessageContentType.ContentType_Typing &&
                contentType != ContentType_Call_End &&
                contentType != ContentType_Call_Accept &&
                contentType != ContentType_Call_Signal &&
                contentType != ContentType_Call_Modify &&
                contentType != ContentType_Call_Accept_T
                ;
    }

    /**
     * 发送消息时也会调用这里，所以这里也需要过滤消息
     * */
    @Override
    public void addProtoMessageByTarget(String target, ProtoMessage protoMessage, boolean isPush) {
        logger.i("add target "+target+" contentType "+protoMessage.getContent().getType());
        if(canPersistent(protoMessage.getContent().getType())){
            insertProtoMessage(target,protoMessage);
            if(isPush){
                //设置未读消息
                int unReadCount = 0;
                if(unReadCountMap.get(protoMessage.getTarget()) != null){
                    unReadCount = unReadCountMap.get(protoMessage.getTarget());
                }
                unReadCountMap.put(protoMessage.getTarget(),++unReadCount);
            }

            createOrUpdateConversation(target,protoMessage.getConversationType(),protoMessage);
        }
    }

    @Override
    public void addProtoMessagesByTarget(String target, List<ProtoMessage> protoMessages, boolean isPush) {
        for(ProtoMessage protoMessage : protoMessages){
            logger.i("add protomessage target "+protoMessage.getTarget()+" messageId "+protoMessage.getMessageId());
            insertProtoMessage(target,protoMessage);
            if(isPush){
                //设置未读消息
                int unReadCount = 0;
                if(unReadCountMap.get(protoMessage.getTarget()) != null){
                    unReadCount = unReadCountMap.get(protoMessage.getTarget());
                }
                unReadCountMap.put(protoMessage.getTarget(),++unReadCount);
            }
        }
        ProtoMessage lastProtoMessage = protoMessages.get(protoMessages.size() -1);
        createOrUpdateConversation(target,lastProtoMessage.getConversationType(),lastProtoMessage);
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target) {
        return getMessages(conversationType,target,0,0,false,20,null);
    }

    @Override
    public ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser) {
        String query = COLUMN_MESSAGE_TARGET + "=" + "'"+target+"'";
        if(fromIndex != 0){
            query = query +" and "+COLUMN_MESSAGE_ID +" < "+fromIndex;
        }
        List<Map<String, Object>> messageList = queryMessageDatabase(query,"message_id DESC LIMIT " + count);
        if(messageList != null){
            int num = messageList.size();
            List<ProtoMessage> protoMessages = new ArrayList<>();
            for(int i = num -1 ;i >= 0; i--){
                Map<String,Object> objectMap = messageList.get(i);
                long messageId = (long) objectMap.get(COLUMN_MESSAGE_ID);
                ProtoMessage protoMessage = (ProtoMessage) objectMap.get(COLUMN_MESSAGE_DATA);
                logger.i("proto messageId "+protoMessage.getMessageId()+" db messageId->"+messageId+" message status "+protoMessage.getStatus()+" contentType "+protoMessage.getContent().getType());
                protoMessages.add(protoMessage);
            }
            ProtoMessage[] protoMessagesArr = new ProtoMessage[protoMessages.size()];
            logger.i("target "+target+" from messageId "+fromIndex+" size "+protoMessages.size());
            return protoMessages.toArray(protoMessagesArr);
        } else {
            return null;
        }

    }

    @Override
    public ProtoMessage getMessage(long messageId) {
        List<Map<String, Object>> res = queryMessageDatabase(COLUMN_MESSAGE_ID + "=" + messageId, null);
        logger.i("getMessage messageId "+messageId+" res "+res);
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
        return protoMessages;
    }

    @Override
    public boolean updateMessageContent(ProtoMessage msg) {
        int retval = -1;
        if(isDatabaseOpen()){
            ProtoMessage updateProtoMessage = getMessage(msg.getMessageId());
            if(updateProtoMessage != null){
                updateProtoMessage.setContent(msg.getContent());
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(COLUMN_MESSAGE_DATA,serialize(updateProtoMessage));
                retval = database.update(ChatStoreHelper.TABLE_MESSAGES,contentValues,COLUMN_MESSAGE_ID + "=" + msg.getMessageId(),null);
                logger.i("update protomessage Content id "+msg.getMessageId());
                createOrUpdateConversation(updateProtoMessage.getTarget(),updateProtoMessage.getConversationType(),updateProtoMessage);
            }

            return retval == 1;
        }
        return false;
    }

    @Override
    public boolean updateMessageStatus(long protoMessageId, int status) {
        int retval = -1;
        if(isDatabaseOpen()){
            ProtoMessage updateProtoMessage = getMessage(protoMessageId);
            if(updateProtoMessage != null){
                updateProtoMessage.setStatus(status);
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(COLUMN_MESSAGE_DATA,serialize(updateProtoMessage));
                retval = database.update(ChatStoreHelper.TABLE_MESSAGES,contentValues,COLUMN_MESSAGE_ID + "=" + protoMessageId,null);
                logger.i("update protomessage status "+status);
                createOrUpdateConversation(updateProtoMessage.getTarget(),updateProtoMessage.getConversationType(),updateProtoMessage);
                return retval == 1;
            }
        }
        return false;
    }

    @Override
    public boolean updateMessageUid(long protoMessageId, long messageUid) {
        logger.i("updateMessageUid protoMessageId "+protoMessageId+" uid "+messageUid);
        int retval = -1;
        if(isDatabaseOpen()){
            ProtoMessage updateProtoMessage = getMessage(protoMessageId);
            if(updateProtoMessage != null){
                updateProtoMessage.setMessageUid(messageUid);
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(COLUMN_MESSAGE_DATA,serialize(updateProtoMessage));
                retval = database.update(ChatStoreHelper.TABLE_MESSAGES,contentValues,COLUMN_MESSAGE_ID + "=" + protoMessageId,null);
                logger.i("update protomessage uid "+messageUid);
                createOrUpdateConversation(updateProtoMessage.getTarget(),updateProtoMessage.getConversationType(),updateProtoMessage);
                return retval == 1;
            }
        }
        return false;
    }

    @Override
    public ProtoMessage getLastMessage(String target) {
        List<Map<String, Object>> res = queryMessageDatabase(COLUMN_MESSAGE_TARGET + "=" + "'"+target+"'", "message_id DESC LIMIT 1");
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
    public void clearUnreadStatus(int conversationType, String target, int line) {
        unReadCountMap.put(target,0);
        ProtoConversationInfo protoConversationInfo  = getConversation(conversationType,target,line);
        if(protoConversationInfo != null){
            ProtoUnreadCount protoUnreadCount = new ProtoUnreadCount();
            protoUnreadCount.setUnread(0);
            protoConversationInfo.setUnreadCount(protoUnreadCount);
            updateConversation(conversationType,target,line,protoConversationInfo);
        }
    }

    @Override
    public int getUnreadCount(String target) {
        return unReadCountMap.get(target) == null ? 0 : unReadCountMap.get(target);
    }

    @Override
    public void removeConversation(int conversationType, String target, int line, boolean clearMsg) {
        if(isDatabaseOpen()){
            int delnum = database.delete(ChatStoreHelper.TABLE_CONVERSATIONS,COLUMN_CONVERSATION_TARGET +"="+"'"+target+"'"+" and "+COLUMN_CONVERSATION_TYPE+"="+conversationType,null);
            logger.i("removeConversation target "+target+" conversation type "+conversationType+" deleted "+(delnum == 1));
        }
    }

    @Override
    public void setConversationDraft(int conversationType, String target, int line, String draft) {
        ProtoConversationInfo protoConversationInfo = getConversation(conversationType,target,line);
        if(protoConversationInfo != null){
            protoConversationInfo.setDraft(draft);
            updateConversation(conversationType,target,line,protoConversationInfo);
        }
    }

    private ProtoConversationInfo createOrUpdateConversation(String target, int type, ProtoMessage lastProtoMessage){
        ProtoConversationInfo protoConversationInfo = newProtoConversation(target,type,lastProtoMessage);
        List<Map<String,Object>> conversation = queryConversation(COLUMN_CONVERSATION_TARGET +"="+"'"+target+"'"+" and "+COLUMN_CONVERSATION_TYPE+"="+type,null);
        if(conversation != null && conversation.size() == 1){
            protoConversationInfo = (ProtoConversationInfo) conversation.get(0).get(COLUMN_CONVERSATION_INFO);
            protoConversationInfo.setLastMessage(lastProtoMessage);
            protoConversationInfo.setTimestamp(System.currentTimeMillis());
            setUnReadCount(protoConversationInfo);
            updateConversation(type,target,0,protoConversationInfo);
        } else {
            insertConversations(target,type,0,protoConversationInfo);
        }
        return protoConversationInfo;
    }

    private boolean updateConversation(int conversationType, String target, int line, ProtoConversationInfo protoConversationInfo){
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(COLUMN_CONVERSATION_INFO,serialize(protoConversationInfo));
        int updateNum = database.update(ChatStoreHelper.TABLE_CONVERSATIONS,contentValues,COLUMN_CONVERSATION_TARGET +"="+"'"+target+"'"+" and "+COLUMN_CONVERSATION_TYPE+"="+conversationType,null);
        logger.i("update conversation "+target+" type "+conversationType+" line "+line+" updated "+(updateNum == 1));
        return updateNum == 1;
    }

    private ProtoConversationInfo newProtoConversation(String target, int type, ProtoMessage lastProtoMessage){
        ProtoConversationInfo protoConversationInfo = new ProtoConversationInfo();
        protoConversationInfo.setConversationType(type);
        protoConversationInfo.setLine(0);
        protoConversationInfo.setTarget(target);
        protoConversationInfo.setTop(conversationSetting(ConversationTop,type,target));
        protoConversationInfo.setSilent(conversationSetting(ConversationSilent,type,target));
        protoConversationInfo.setLastMessage(lastProtoMessage);
        setUnReadCount(protoConversationInfo);
        protoConversationInfo.setTimestamp(System.currentTimeMillis());
        return protoConversationInfo;
    }

    private void setUnReadCount(ProtoConversationInfo protoConversationInfo){
        ProtoUnreadCount protoUnreadCount = new ProtoUnreadCount();
        protoUnreadCount.setUnread(getUnreadCount(protoConversationInfo.getTarget()));
        protoConversationInfo.setUnreadCount(protoUnreadCount);
    }

    private boolean conversationSetting(int scope,int conversationType,String target){
        boolean flag = false;
        String top = getUserSetting(scope,conversationType + "-" + 0 + "-" + target);
        if(!TextUtils.isEmpty(top)){
            flag = top.equals("1");
        }
        logger.i(scope+"-"+conversationType + "-" + 0 + "-" + target+" flag->"+flag);
        return flag;
    }

    @Override
    public List<ProtoConversationInfo> getPrivateConversations() {
        logger.i("getPrivateConversations");
        List<Map<String,Object>> privateConversations = queryConversation(COLUMN_CONVERSATION_TYPE +"="+ Conversation.ConversationType.Single.ordinal(),null);
        List<ProtoConversationInfo> protoConversationInfoList = new ArrayList<>();
        for(Map<String,Object> objectMap : privateConversations){
            ProtoConversationInfo protoConversationInfo = (ProtoConversationInfo) objectMap.get(COLUMN_CONVERSATION_INFO);
            protoConversationInfoList.add(protoConversationInfo);
        }
        return protoConversationInfoList;
    }

    @Override
    public List<ProtoConversationInfo> getGroupConversations() {
        logger.i("getGroupConversations");
        List<Map<String,Object>> groupConversations = queryConversation(COLUMN_CONVERSATION_TYPE +"="+ Conversation.ConversationType.Group.ordinal(),null);
        List<ProtoConversationInfo> protoConversationInfoList = new ArrayList<>();
        for(Map<String,Object> objectMap : groupConversations){
            ProtoConversationInfo protoConversationInfo = (ProtoConversationInfo) objectMap.get(COLUMN_CONVERSATION_INFO);
            protoConversationInfoList.add(protoConversationInfo);
        }
        return protoConversationInfoList;
    }

    @Override
    public ProtoConversationInfo getConversation(int conversationType, String target, int line) {
        logger.i("getConversation type "+conversationType+" target "+target);
        ProtoConversationInfo protoConversationInfo = new ProtoConversationInfo();
        if(!TextUtils.isEmpty(target)){
            List<Map<String,Object>> conversation = queryConversation(COLUMN_CONVERSATION_TARGET +"="+"'"+target+"'"+" and "+COLUMN_CONVERSATION_TYPE+"="+conversationType,null);
            if(conversation != null && conversation.size() == 1){
                protoConversationInfo = (ProtoConversationInfo) conversation.get(0).get(COLUMN_CONVERSATION_INFO);
            } else {
                protoConversationInfo = newProtoConversation(target,conversationType,null);
                insertConversations(target,conversationType,0,protoConversationInfo);
            }
        }
        return protoConversationInfo;
    }

    @Override
    public ProtoConversationInfo[] getConversations(int[] conversationTypes, int[] lines) {
        return new ProtoConversationInfo[0];
    }

    @Override
    public void setUserSetting(int scope, String key, String value) {
        Map<String,String> scopeMap = userSettingMap.get(scope);
        if(scopeMap == null){
            scopeMap = new HashMap<>();
        }
        scopeMap.put(key,value);
        userSettingMap.put(scope,scopeMap);
        if(!TextUtils.isEmpty(key)){
            String[] keys = key.split("-");
            int conversationType = Integer.parseInt(keys[0]);
            int line = Integer.parseInt(keys[1]);
            String target = keys[2];
            ProtoConversationInfo updateProtoConversation = getConversation(conversationType,target,line);
            if(scope == ConversationTop){
                updateProtoConversation.setTop(value.equals("1") ? true : false);
            } else if(scope == ConversationSilent){
                updateProtoConversation.setSilent(value.equals("1") ? true : false);
            }
            updateConversation(conversationType,target,0,updateProtoConversation);
        }
    }

    @Override
    public String getUserSetting(int scope, String key) {
        Map<String,String> scopeMap = userSettingMap.get(scope);
        if(scopeMap != null){
            return scopeMap.get(key);
        }
        return null;
    }

    @Override
    public Map<String, String> getUserSettings(int scope) {
        return userSettingMap.get(scope);
    }
}
