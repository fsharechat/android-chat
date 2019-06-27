package cn.wildfirechat.proto;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cn.wildfirechat.alarm.AlarmWrapper;
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

    public static void init(AlarmWrapper alarmWrapper){
        logger.i("init proto service");
        protoService = new ProtoService(alarmWrapper);
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

    }

    public static long insertMessage(ProtoMessage msg){
        return 0;
    }

    public static void updateMessageContent(ProtoMessage msg){

    }

    public static long getServerDeltaTime(){
        return 0;
    }

    public static  ProtoConversationInfo[] getConversations(int[] conversationTypes, int[] lines){
        List<ProtoConversationInfo> protoConversationInfos = new ArrayList<>();
        for(int conversationType : conversationTypes){
            if(conversationType == ProtoConstants.ConversationType.ConversationType_Private){
                protoConversationInfos.addAll(protoService.getImMemoryStore().getPrivateConversations());
            } else if(conversationType == ProtoConstants.ConversationType.ConversationType_Group){
                protoConversationInfos.addAll(protoService.getImMemoryStore().getGroupConversations());
            }
        }
        ProtoConversationInfo[] protoConversationInfosArr = new ProtoConversationInfo[protoConversationInfos.size()];
        protoConversationInfos.toArray(protoConversationInfosArr);
        return protoConversationInfosArr;

    }

//    public static ProtoConversationInfo[] getPrivateConversations(){
//        String[] friendList = protoService.getMyFriendList(false);
//        if(friendList != null){
//            ProtoConversationInfo[] protoConversationInfos = new ProtoConversationInfo[friendList.length];
//            int i = 0;
//            for(String friend : friendList){
//                ProtoConversationInfo protoConversationInfo = new ProtoConversationInfo();
//                protoConversationInfo.setConversationType(Conversation.ConversationType.Single.ordinal());
//                protoConversationInfo.setLine(0);
//                protoConversationInfo.setTarget(friend);
//                ProtoMessage protoMessage = protoService.getImMemoryStore().getLastMessage(friend);
//                if(protoMessage != null &&(!TextUtils.isEmpty(protoMessage.getContent().getPushContent())
//                        || !TextUtils.isEmpty(protoMessage.getContent().getSearchableContent())) ){
//                    protoMessage.setStatus(5);
//                }
//                protoConversationInfo.setLastMessage(protoMessage);
//
//                ProtoUnreadCount protoUnreadCount = new ProtoUnreadCount();
//                protoUnreadCount.setUnread(protoService.getImMemoryStore().getUnreadCount(friend));
//                //logger.i("friend "+friend+" unread "+protoService.getImMemoryStore().getUnreadCount(friend));
//                protoConversationInfo.setUnreadCount(protoUnreadCount);
//                protoConversationInfo.setTimestamp(System.currentTimeMillis());
//                protoConversationInfos[i++] =protoConversationInfo;
//            }
//            return protoConversationInfos;
//        } else {
//            return new ProtoConversationInfo[0];
//        }
//    }

    public static  ProtoConversationInfo getConversation(int conversationType, String target, int line){
        //return protoService.getImMemoryStore().getConversation(conversationType,target,line);
        int[] conversationTypes = new int[1];
        int[] lines = new int[1];
        conversationTypes[0] = conversationType;
        lines[0] = line;
        ProtoConversationInfo[] protoConversationInfos = getConversations(conversationTypes,lines);
        if(protoConversationInfos != null){
            for(ProtoConversationInfo protoConversationInfo : protoConversationInfos){
                if(protoConversationInfo.getTarget().equals(target)){
                    return protoConversationInfo;
                }
            }
        }
        return new ProtoConversationInfo();
    }

    public static  ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser){
        return protoService.getMessages(conversationType,target,line,fromIndex,before,count,withUser);
    }

    public static void getRemoteMessages(int conversationType, String target, int line, long beforeMessageUid, int count, JavaProtoLogic.ILoadRemoteMessagesCallback callback){
        protoService.getRemoteMessages(conversationType,target,line,beforeMessageUid,count,callback);
    }

    public static  ProtoMessage getMessage(long messageId){
        return new ProtoMessage();
    }

    public static ProtoMessage getMessageByUid(long messageUid){
        return new ProtoMessage();
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

    public static void setMediaMessagePlayed(long messageId){}

    public static void removeConversation(int conversationType, String target, int line, boolean clearMsg){}

    public static void setConversationTop(int conversationType, String target, int line, boolean top){}

    public static void setConversationDraft(int conversationType, String target, int line, String draft){}

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

    //- (void)uploadMedia:(NSData *)mediaData mediaType:(WFCCMediaType)mediaType success:(void(^)(NSString *remoteUrl))successBlock error:(void(^)(int error_code))errorBlock
    public static void uploadMedia(byte[] data, int mediaType, JavaProtoLogic.IUploadMediaCallback callback){}

    //-(void)modifyMyInfo:(NSDictionary<NSNumber */*ModifyMyInfoType*/, NSString *> *)values
    //success:(void(^)())successBlock
    //error:(void(^)(int error_code))errorBlock
    public static void modifyMyInfo(Map<Integer, String> values, JavaProtoLogic.IGeneralCallback callback){}

    //- (BOOL)deleteMessage:(long)messageId
    public static boolean deleteMessage(long messageId){
        return false;
    }

    //- (NSArray<WFCCConversationSearchInfo *> *)searchConversation:(NSString *)keyword
    public static ProtoConversationSearchresult[] searchConversation(String keyword, int[] conversationTypes, int[] lines){
        return new ProtoConversationSearchresult[0];
    }

    //- (NSArray<WFCCMessage *> *)searchMessage:(WFCCConversation *)conversation keyword:(NSString *)keyword
    public static ProtoMessage[] searchMessage(int conversationType, String target, int line, String keyword){
        return new ProtoMessage[0];
    }

    //- (NSArray<WFCCUserInfo *> *)searchFriends:(NSString *)keyword;
    public static ProtoUserInfo[] searchFriends(String keyword){
        return new ProtoUserInfo[0];
    }

    //- (NSArray<WFCCGroupSearchInfo *> *)searchGroups:(NSString *)keyword;
    public static ProtoGroupSearchResult[] searchGroups(String keyword){
        return new ProtoGroupSearchResult[0];
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

    public static void modifyGroupInfo(String groupId, int modifyType, String newValue, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){}

    //- (void)modifyGroupAlias:(NSString *)groupId
//    alias:(NSString *)newAlias
//    notifyLines:(NSArray<NSNumber *> *)notifyLines
//    notifyContent:(WFCCMessageContent *)notifyContent
//    success:(void(^)())successBlock
//    error:(void(^)(int error_code))errorBlock;
    public static void modifyGroupAlias(String groupId, String newAlias, int[] notifyLines, ProtoMessageContent notifyMsg, JavaProtoLogic.IGeneralCallback callback){}


    public static ProtoGroupMember[] getGroupMembers(String groupId, boolean forceUpdate){
        return protoService.getGroupMembers(groupId,forceUpdate);
    }

    public static ProtoGroupMember getGroupMember(String groupId, String memberId){
        return protoService.getGroupMember(groupId,memberId);
    }

    //- (void)transferGroup:(NSString *)groupId
//                   to:(NSString *)newOwner
//          notifyLines:(NSArray<NSNumber *> *)notifyLines
//        notifyContent:(WFCCMessageContent *)notifyContent
//              success:(void(^)())successBlock
//                error:(void(^)(int error_code))errorBlock;
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
    //
//- (void)searchChannel:(NSString *)keyword success:(void(^)(NSArray<WFCCChannelInfo *> *machedChannels))successBlock error:(void(^)(int errorCode))errorBlock;
    public static void searchChannel(String keyword, JavaProtoLogic.ISearchChannelCallback callback){}

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
        return null;
    }
    public static Map<String, String> getUserSettings(int scope){
        return new HashMap<>();
    }
    public static void setUserSetting(int scope, String key, String value, JavaProtoLogic.IGeneralCallback callback){}
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
