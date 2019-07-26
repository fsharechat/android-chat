package cn.wildfirechat.proto;
import android.content.Context;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import cn.wildfirechat.alarm.AlarmWrapper;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.ProtoChannelInfo;
import cn.wildfirechat.model.ProtoChatRoomInfo;
import cn.wildfirechat.model.ProtoChatRoomMembersInfo;
import cn.wildfirechat.model.ProtoConversationInfo;
import cn.wildfirechat.model.ProtoConversationSearchresult;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.model.ProtoGroupSearchResult;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUnreadCount;
import cn.wildfirechat.model.ProtoUserInfo;

public class JavaProtoLogic {

    private static ProtoService protoService;
    private static Log logger = LoggerFactory.getLogger(JavaProtoLogic.class);

    public interface IConnectionStatusCallback {
        void onConnectionStatusChanged(int status);
    }

    public interface IReceiveMessageCallback {
        void onReceiveMessage(List<ProtoMessage> messages, boolean hasMore);
        void onRecallMessage(long messageUid);
    }

    public interface ILoadRemoteMessagesCallback {
        void onSuccess(ProtoMessage[] messages);
        void onFailure(int errorCode);
    }

    public interface IGroupInfoUpdateCallback {
        void onGroupInfoUpdated(List<ProtoGroupInfo> updatedGroupInfos);
    }

    public interface IGroupMembersUpdateCallback {
        void onGroupMembersUpdated(String updatedGroupId, List<ProtoGroupMember> members);
    }

    public interface IChannelInfoUpdateCallback {
        void onChannelInfoUpdated(List<ProtoChannelInfo> updatedChannelInfos);
    }

    public interface IUserInfoUpdateCallback {
        void onUserInfoUpdated(List<ProtoUserInfo> updatedUserInfos);
    }

    public interface IFriendListUpdateCallback {
        void onFriendListUpdated(String[] updatedFriendList);
    }

    public interface IFriendRequestListUpdateCallback {
        void onFriendRequestUpdated();
    }

    public interface ISettingUpdateCallback {
        void onSettingUpdated();
    }

    public interface IGeneralCallback {
        void onSuccess();
        void onFailure(int errorCode);
    }

    public interface IGeneralCallback2 {
        void onSuccess(String retId);
        void onFailure(int errorCode);
    }

    public interface IGeneralCallback3 {
        void onSuccess(String[] retId);
        void onFailure(int errorCode);
    }

    public interface ISendMessageCallback {
        void onSuccess(long messageUid, long timestamp);
        void onFailure(int errorCode);
        void onPrepared(long messageId, long savedTime);
        void onProgress(long uploaded, long total);
        void onMediaUploaded(String remoteUrl);
    }

    public interface ISearchUserCallback {
        void onSuccess(ProtoUserInfo[] userInfos);
        void onFailure(int errorCode);
    }

    public interface IUploadMediaCallback {
        void onSuccess(String remoteUrl);
        void onProgress(long uploaded, long total);
        void onFailure(int errorCode);
    }

    public interface IGetChatRoomInfoCallback {
        void onSuccess(ProtoChatRoomInfo chatRoomInfo);
        void onFailure(int errorCode);
    }


    public interface IGetChatRoomMembersInfoCallback {
        void onSuccess(ProtoChatRoomMembersInfo chatRoomMembersInfo);
        void onFailure(int errorCode);
    }

    public interface ICreateChannelCallback {
        void onSuccess(ProtoChannelInfo channelInfo);
        void onFailure(int errorCode);
    }

    public interface ISearchChannelCallback {
        void onSuccess(ProtoChannelInfo[] channelInfos);
        void onFailure(int errorCode);
    }

    private static JavaProtoLogic.IConnectionStatusCallback connectionStatusCallback = null;
    private static JavaProtoLogic.IReceiveMessageCallback receiveMessageCallback = null;
    private static JavaProtoLogic.IGroupInfoUpdateCallback groupInfoUpdateCallback = null;
    private static JavaProtoLogic.IGroupMembersUpdateCallback groupMembersUpdateCallback = null;

    private static JavaProtoLogic.IUserInfoUpdateCallback userInfoUpdateCallback = null;
    private static JavaProtoLogic.IFriendListUpdateCallback friendListUpdateCallback = null;
    private static JavaProtoLogic.IFriendRequestListUpdateCallback friendRequestListUpdateCallback = null;
    private static JavaProtoLogic.ISettingUpdateCallback settingUpdateCallback = null;
    private static JavaProtoLogic.IChannelInfoUpdateCallback channelInfoUpdateCallback = null;

    public static void setConnectionStatusCallback(JavaProtoLogic.IConnectionStatusCallback connectionStatusCallback) {
        JavaProtoLogic.connectionStatusCallback = connectionStatusCallback;
    }

    public static void setReceiveMessageCallback(JavaProtoLogic.IReceiveMessageCallback receiveMessageCallback) {
        JavaProtoLogic.receiveMessageCallback = receiveMessageCallback;
    }

    public static void setGroupInfoUpdateCallback(JavaProtoLogic.IGroupInfoUpdateCallback groupInfoUpdateCallback) {
        JavaProtoLogic.groupInfoUpdateCallback = groupInfoUpdateCallback;
    }

    public static void setGroupMembersUpdateCallback(JavaProtoLogic.IGroupMembersUpdateCallback groupMembersUpdateCallback) {
        JavaProtoLogic.groupMembersUpdateCallback = groupMembersUpdateCallback;
    }

    public static void setChannelInfoUpdateCallback(JavaProtoLogic.IChannelInfoUpdateCallback channelInfoUpdateCallback) {
        JavaProtoLogic.channelInfoUpdateCallback = channelInfoUpdateCallback;
    }

    public static void setUserInfoUpdateCallback(JavaProtoLogic.IUserInfoUpdateCallback userInfoUpdateCallback) {
        JavaProtoLogic.userInfoUpdateCallback = userInfoUpdateCallback;
    }

    public static void setFriendListUpdateCallback(JavaProtoLogic.IFriendListUpdateCallback friendListUpdateCallback) {
        JavaProtoLogic.friendListUpdateCallback = friendListUpdateCallback;
    }

    public static void setFriendRequestListUpdateCallback(JavaProtoLogic.IFriendRequestListUpdateCallback friendRequestListUpdateCallback) {
        JavaProtoLogic.friendRequestListUpdateCallback = friendRequestListUpdateCallback;
    }

    public static void setSettingUpdateCallback(JavaProtoLogic.ISettingUpdateCallback settingUpdateCallback) {
        JavaProtoLogic.settingUpdateCallback = settingUpdateCallback;
    }

    public static final String TAG = "mars.StnLogic";

    public static void onConnectionStatusChanged(int status) {
        if(connectionStatusCallback != null) {
            connectionStatusCallback.onConnectionStatusChanged(status);
        }
    }

    public static void onReceiveMessage(ProtoMessage[] messages, boolean hasMore) {
        if (receiveMessageCallback != null) {
            List<ProtoMessage> list = new ArrayList<>();
            for (ProtoMessage protoMsg: messages
            ) {
                list.add(protoMsg);
            }

            receiveMessageCallback.onReceiveMessage(list, hasMore);
        }
    }

    public static void onRecallMessage(long messageUid) {
        if (receiveMessageCallback != null) {
            receiveMessageCallback.onRecallMessage(messageUid);
        }
    }

    public static void onGroupInfoUpdated(ProtoGroupInfo[] groupInfos) {
        if (groupInfoUpdateCallback != null) {
            List<ProtoGroupInfo> list = new ArrayList<>();
            for (ProtoGroupInfo protoUserInfo : groupInfos
            ) {
                list.add(protoUserInfo);
            }
            groupInfoUpdateCallback.onGroupInfoUpdated(list);
        }
    }

    public static void onGroupMembersUpdated(String groupId, ProtoGroupMember[] members) {
        if (groupMembersUpdateCallback != null) {
            List<ProtoGroupMember> list = new ArrayList<>();
            for (ProtoGroupMember member : members) {
                list.add(member);
            }
            groupMembersUpdateCallback.onGroupMembersUpdated(groupId, list);
        }
    }

    public static void onChannelInfoUpdated(ProtoChannelInfo[] channelInfos) {
        if (channelInfoUpdateCallback != null) {
            List<ProtoChannelInfo> list = new ArrayList<>();
            for (ProtoChannelInfo protoChannelInfo : channelInfos
            ) {
                list.add(protoChannelInfo);
            }
            channelInfoUpdateCallback.onChannelInfoUpdated(list);
        }
    }

    public static void onUserInfoUpdated(ProtoUserInfo[] userInfos) {
        if (userInfoUpdateCallback != null) {
            List<ProtoUserInfo> list = new ArrayList<>();
            for (ProtoUserInfo protoUserInfo : userInfos
            ) {
                list.add(protoUserInfo);
            }
            userInfoUpdateCallback.onUserInfoUpdated(list);
        }
    }

    public static void onFriendListUpdated(String[] friendList) {
        if (friendListUpdateCallback != null) {
            friendListUpdateCallback.onFriendListUpdated(friendList);
        }
    }

    public static void onFriendRequestUpdated() {
        if (friendRequestListUpdateCallback != null) {
            friendRequestListUpdateCallback.onFriendRequestUpdated();
        }
    }

    public static void onSettingUpdated() {
        if (settingUpdateCallback != null) {
            settingUpdateCallback.onSettingUpdated();
        }
    }

    public static void init(Context context,AlarmWrapper alarmWrapper){
        logger.i("init proto service");
        protoService = new ProtoService(context,alarmWrapper);
    }

    public static void stopProtoService(){
        protoService.stopProtoService();
    }

    public static void connect(String host, int shortPort){
        logger.i("connect host "+host+" port "+shortPort);
        protoService.connect(host,shortPort);
    }

    public static void reconnect(){
        if(protoService != null){
            protoService.reconnect();
        }
    }

    public static void tryHeartbeat(){
        if(protoService != null){
            protoService.tryHeartbeat();
        }
    }

    public static void setAuthInfo(String userName, String token){
        protoService.setUserName(userName);
        protoService.setToken(token);
    }

    public static void disconnect(int flag){
        protoService.disConnect(flag);
    }

    public static int getConnectionStatus(){
        return 0;
    }

    public static void sendMessage(ProtoMessage msg, int expireDuration, JavaProtoLogic.ISendMessageCallback callback){
        protoService.sendMessage(msg,expireDuration,callback);
    }

    public static void recallMessage(long messageUid, JavaProtoLogic.IGeneralCallback callback){
       protoService.recallMessage(messageUid,callback);
    }

    public static long insertMessage(ProtoMessage msg){
        return 0;
    }

    public static void updateMessageContent(ProtoMessage msg){
        protoService.updateMessageContent(msg);
    }

    public static long getServerDeltaTime(){
        return 0;
    }

    public static  ProtoConversationInfo[] getConversations(int[] conversationTypes, int[] lines){
        List<ProtoConversationInfo> protoConversationInfos = new ArrayList<>();
        List<ProtoConversationInfo> privateConversationInfos = null;
        List<ProtoConversationInfo> groupProtoConversationInfos = null;
        for(int conversationType : conversationTypes){
            if(conversationType == ProtoConstants.ConversationType.ConversationType_Private){
                privateConversationInfos = protoService.getImMemoryStore().getPrivateConversations();
                Iterator<ProtoConversationInfo> privateIterator = privateConversationInfos.iterator();
                while (privateIterator.hasNext()){
                    ProtoConversationInfo privateProtoConversationInfo = privateIterator.next();
                    if(privateProtoConversationInfo.isTop()){
                        privateIterator.remove();
                        protoConversationInfos.add(privateProtoConversationInfo);
                    }
                }
            } else if(conversationType == ProtoConstants.ConversationType.ConversationType_Group){
                groupProtoConversationInfos = protoService.getImMemoryStore().getGroupConversations();
                Iterator<ProtoConversationInfo> groupIterator = groupProtoConversationInfos.iterator();
                while (groupIterator.hasNext()){
                    ProtoConversationInfo groupProtoConversationInfo = groupIterator.next();
                    if(groupProtoConversationInfo.isTop()){
                        groupIterator.remove();
                        protoConversationInfos.add(groupProtoConversationInfo);
                    }
                }
            }
        }
        if(privateConversationInfos != null && privateConversationInfos.size() > 0){
            protoConversationInfos.addAll(privateConversationInfos);
        }
        if(groupProtoConversationInfos != null && groupProtoConversationInfos.size() > 0){
            protoConversationInfos.addAll(groupProtoConversationInfos);
        }
        ProtoConversationInfo[] protoConversationInfosArr = new ProtoConversationInfo[protoConversationInfos.size()];
        protoConversationInfos.toArray(protoConversationInfosArr);
        return protoConversationInfosArr;

    }

    public static  ProtoConversationInfo getConversation(int conversationType, String target, int line){
        ProtoConversationInfo protoConversationInfo = protoService.getImMemoryStore().getConversation(conversationType,target,line);
        protoConversationInfo.setTimestamp(System.currentTimeMillis());
        return protoConversationInfo;
    }

    public static  ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser){
        return protoService.getMessages(conversationType,target,line,fromIndex,before,count,withUser);
    }

    public static void getRemoteMessages(int conversationType, String target, int line, long beforeMessageUid, int count, JavaProtoLogic.ILoadRemoteMessagesCallback callback){
        protoService.getRemoteMessages(conversationType,target,line,beforeMessageUid,count,callback);
    }

    public static  ProtoMessage getMessage(long messageId){
        return protoService.getMessage(messageId);
    }

    public static ProtoMessage getMessageByUid(long messageUid){
        return protoService.getMessageByUid(messageUid);
    }

    public static ProtoUnreadCount getUnreadCount(int conversationType, String target, int line){
        ProtoUnreadCount protoUnreadCount = new ProtoUnreadCount();
        protoUnreadCount.setUnread(1);
        return protoUnreadCount;
    }

    public static ProtoUnreadCount getUnreadCountEx(int[] conversationTypes, int[] lines){
        return new ProtoUnreadCount();
    }

    public static void clearUnreadStatus(int conversationType, String target, int line){
        protoService.getImMemoryStore().clearUnreadStatus(conversationType,target,line);
    }

    public static void clearAllUnreadStatus(){}

    public static void clearMessages(int conversationType, String target, int line){}

    public static void setMediaMessagePlayed(long messageId){
       protoService.getImMemoryStore().updateMessageStatus(messageId, MessageStatus.Played.ordinal());
    }

    public static void removeConversation(int conversationType, String target, int line, boolean clearMsg){
        protoService.getImMemoryStore().removeConversation(conversationType,target,line,clearMsg);
    }

    public static void setConversationTop(int conversationType, String target, int line, boolean top){
        logger.i("setConversationTop "+conversationType+" target "+target+" top "+false);
    }

    public static void setConversationDraft(int conversationType, String target, int line, String draft){
        protoService.getImMemoryStore().setConversationDraft(conversationType,target,line,draft);
    }

    public static void setConversationSilent(int conversationType, String target, int line, boolean silent){}

    public static void searchUser(String keyword, JavaProtoLogic.ISearchUserCallback callback){
        protoService.searchUser(keyword,callback);
    }


    public static boolean isMyFriend(String userId){
        return protoService.isMyFriend(userId);
    }

    public static String[] getMyFriendList(boolean refresh){
        return protoService.getMyFriendList(refresh);
    }

    public static String getFriendAlias(String userId){
        return null;
    }

    public static void setFriendAlias(String userId, String alias, JavaProtoLogic.IGeneralCallback callback){}

    //- (void)loadFriendRequestFromRemote
    public static void loadFriendRequestFromRemote(){}

    //- (NSArray<WFCCFriendRequest *> *)getIncommingFriendRequest
    public static ProtoFriendRequest[] getFriendRequest(boolean incomming){
        return protoService.getFriendRequest(incomming);
    }

    //- (void)clearUnreadFriendRequestStatus
    public static void clearUnreadFriendRequestStatus(){}

    public static int getUnreadFriendRequestStatus(){
        return protoService.getUnreadFriendRequestStatus();
    }

    //- (void)removeFriend:(NSString *)userId
    //success:(void(^)())successBlock
    //error:(void(^)(int error_code))errorBlock
    public static void removeFriend(String userId, JavaProtoLogic.IGeneralCallback callback){}


    public static void sendFriendRequest(String userId, String reason, JavaProtoLogic.IGeneralCallback callback){
        protoService.sendFriendRequest(userId,reason,callback);
    }



    public static void handleFriendRequest(String userId, boolean accept, JavaProtoLogic.IGeneralCallback callback){
        protoService.handleFriendRequest(userId,accept,callback);
    }

    //- (void)deleteFriend:(NSString *)userId
    //             success:(void(^)())successBlock
    //               error:(void(^)(int error_code))errorBlock {
    public static void deleteFriend(String userId, JavaProtoLogic.IGeneralCallback callback){}


    //    - (BOOL)isBlackListed:(NSString *)userId;
    public static boolean isBlackListed(String userId){
        return false;
    }

    //    - (NSArray<NSString *> *)getBlackList:(BOOL)refresh;
    public static String[] getBlackList(boolean refresh){
        return new String[0];
    }

    //- (void)setBlackList:(NSString *)userId
//    isBlackListed:(BOOL)isBlackListed
//    success:(void(^)(void))successBlock
//    error:(void(^)(int error_code))errorBlock;
    public static void setBlackList(String userId, boolean isBlackListed, JavaProtoLogic.IGeneralCallback callback){}

    public static ProtoUserInfo getUserInfo(String userId, String groupId, boolean refresh){
        return protoService.getUserInfo(userId,groupId,refresh);
    }
    public static ProtoUserInfo[] getUserInfos(String[] userIds, String groupId){
        return protoService.getUserInfos(userIds,groupId);
    }

    public static void uploadMedia(byte[] data, int mediaType, JavaProtoLogic.IUploadMediaCallback callback){
        protoService.uploadMedia(data,mediaType,callback);
    }


    public static void modifyMyInfo(Map<Integer, String> values, JavaProtoLogic.IGeneralCallback callback){
        protoService.modifyMyInfo(values,callback);
    }

    public static boolean deleteMessage(long messageId){
        return protoService.deleteMessage(messageId);
    }


    public static ProtoConversationSearchresult[] searchConversation(String keyword, int[] conversationTypes, int[] lines){
        return protoService.getImMemoryStore().searchConversation(keyword,conversationTypes,lines);
    }

    public static ProtoMessage[] searchMessage(int conversationType, String target, int line, String keyword){
        return protoService.getImMemoryStore().searchMessage(conversationType,target,line,keyword);
    }

    public static ProtoUserInfo[] searchFriends(String keyword){
        return protoService.getImMemoryStore().searchFriends(keyword);
    }

    public static ProtoGroupSearchResult[] searchGroups(String keyword){
        return protoService.getImMemoryStore().searchGroups(keyword);
    }

    public static void createGroup(String groupId, String groupName, String groupPortrait, String[] memberIds, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback2 callback){
        logger.i("createGroup groupId "+groupId+" groupName "+groupName +" groupPortrait "+groupPortrait+" notifyMsg "+ (notifyMsg != null));
        protoService.createGroup(groupId,groupName,groupPortrait,memberIds,notifyLines,notifyMsg,callback);
    }

    public static void addMembers(String groupId, String[] memberIds, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
       protoService.addMembers(groupId,memberIds,notifyLines,notifyMsg,callback);
    }

    public static void kickoffMembers(String groupId, String[] memberIds, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
        protoService.kickoffMembers(groupId,memberIds,notifyLines,notifyMsg,callback);
    }

    public static void quitGroup(String groupId, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
        protoService.quitGroup(groupId,notifyLines,notifyMsg,callback);
    }


    public static void dismissGroup(String groupId, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){}

    public static void modifyGroupInfo(String groupId, int modifyType, String newValue, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){
        protoService.modifyGroupInfo(groupId,modifyType,newValue,notifyLines,notifyMsg,callback);
    }

    public static void modifyGroupAlias(String groupId, String newAlias, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){}


    public static ProtoGroupMember[] getGroupMembers(String groupId, boolean forceUpdate){
        return protoService.getGroupMembers(groupId,forceUpdate);
    }

    public static ProtoGroupMember getGroupMember(String groupId, String memberId){
        return protoService.getGroupMember(groupId,memberId);
    }

    public static void transferGroup(String groupId, String newOwner, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){}

    //    - (void)createChannel:(NSString *)channelName
//    portrait:(NSString *)channelPortrait
//    status:(int)status
//    desc:(NSString *)desc
//    extra:(NSString *)extra
//    success:(void(^)(WFCCChannelInfo *channelInfo))successBlock
//    error:(void(^)(int error_code))errorBlock;
    public static void createChannel(String channelId, String channelName, String channelPortrait, int status, String desc, String extra, JavaProtoLogic.ICreateChannelCallback callback){}

    //- (WFCCChannelInfo *)getChannelInfo:(NSString *)channelId
//    refresh:(BOOL)refresh;
    public static ProtoChannelInfo getChannelInfo(String channelId, boolean refresh){
        return new ProtoChannelInfo();
    }

    public static void searchChannel(String keyword, JavaProtoLogic.ISearchChannelCallback callback){
        callback.onSuccess(new ProtoChannelInfo[0]);
    }

    public static void modifyChannelInfo(String channelId, int modifyType, String newValue, JavaProtoLogic.IGeneralCallback callback){}

    //- (BOOL)isListened:(NSString *)channelId;
    public static boolean isListenedChannel(String channelId){
        return false;
    }
    //
//- (void)listenChannel:(NSString *)channelId listen:(BOOL)listen success:(void(^)(void))successBlock error:(void(^)(int errorCode))errorBlock;
    public static void listenChannel(String channelId, boolean listen, JavaProtoLogic.IGeneralCallback callback){}
    //
//- (NSArray<NSString *> *)getMyChannels;
    public static String[] getMyChannels(){
        return new String[0];
    }
    //
//- (NSArray<NSString *> *)getListenedChannels;
    public static String[] getListenedChannels(){
        return new String[0];
    }

    //- (void)destoryChannel:(NSString *)channelId success:(void(^)(void))successBlock error:(void(^)(int error_code))errorBlock
    public static void destoryChannel(String channelId, JavaProtoLogic.IGeneralCallback callback){}

    public static String getUserSetting(int scope, String key){
        return protoService.getImMemoryStore().getUserSetting(scope,key);
    }
    public static Map<String, String> getUserSettings(int scope){
        return protoService.getImMemoryStore().getUserSettings(scope);
    }
    public static void setUserSetting(int scope, String key, String value, JavaProtoLogic.IGeneralCallback callback){
        logger.i("setUserSetting scope "+scope+" key "+key+" value "+value);
        protoService.getImMemoryStore().setUserSetting(scope,key,value);
    }
    public static void setDeviceToken(String appName, String token, int pushType){}
    public static void setDNSResult(String[] serverIPs){}

    public static ProtoGroupInfo getGroupInfo(String groupId, boolean refresh){
        return protoService.getGroupInfo(groupId,refresh);
    }

    public static void getChatRoomInfo(String chatRoomId, long lastUpdateDt, JavaProtoLogic.IGetChatRoomInfoCallback callback){}
    public static void getChatRoomMembersInfo(String chatRoomId, int maxCount, JavaProtoLogic.IGetChatRoomMembersInfoCallback callback){}
    public static void joinChatRoom(String chatRoomId, JavaProtoLogic.IGeneralCallback callback){}
    public static void quitChatRoom(String chatRoomId, JavaProtoLogic.IGeneralCallback callback){}

    public static void registerMessageFlag(int contentType, int flag){}
    /**
     * 获取底层已加载模块
     * @return
     */
    private static native ArrayList<String> getLoadLibraries();
}
