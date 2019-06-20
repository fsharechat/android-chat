package cn.wildfirechat.proto;

import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.comsince.github.client.AndroidNIOClient;
import com.comsince.github.client.ConnectStatus;
import com.comsince.github.client.PushMessageCallback;
import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.comsince.github.push.util.AES;
import com.comsince.github.push.util.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.handler.AbstractMessagHandler;
import cn.wildfirechat.proto.handler.AddFriendRequestHandler;
import cn.wildfirechat.proto.handler.ConnectAckMessageHandler;
import cn.wildfirechat.proto.handler.FriendPullHandler;
import cn.wildfirechat.proto.handler.FriendRequestHandler;
import cn.wildfirechat.proto.handler.GetUserInfoMessageHanlder;
import cn.wildfirechat.proto.handler.HandlerFriendRequestHandler;
import cn.wildfirechat.proto.handler.MessageHandler;
import cn.wildfirechat.proto.handler.NotifyFriendHandler;
import cn.wildfirechat.proto.handler.NotifyFriendRequestHandler;
import cn.wildfirechat.proto.handler.NotifyMessageHandler;
import cn.wildfirechat.proto.handler.ReceiveMessageHandler;
import cn.wildfirechat.proto.handler.RequestInfo;
import cn.wildfirechat.proto.handler.SearchUserResultMessageHandler;
import cn.wildfirechat.proto.handler.SendMessageHandler;
import cn.wildfirechat.proto.model.ConnectMessage;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnected;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnecting;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusUnconnected;
import static com.tencent.mars.comm.PlatformComm.context;

public class ProtoService implements PushMessageCallback {
    public static Log log = LoggerFactory.getLogger(ProtoService.class);
    private String userName;
    private String token;
    private AndroidNIOClient androidNIOClient;

    private List<MessageHandler> messageHandlers = new ArrayList<>();
    public ConcurrentHashMap<Integer, RequestInfo> requestMap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer, SimpleFuture> futureMap = new ConcurrentHashMap<>();
    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public String[] myFriendList;
    private volatile boolean userDisconnect = false;
    private Object reconectScheduled;
    private Object heartbeatScheduled;

    public ProtoService(){
        initHandlers();
    }

    public void connect(String host, int shortPort){
        userDisconnect = false;
        androidNIOClient = new AndroidNIOClient(host,shortPort);
        androidNIOClient.setPushMessageCallback(this);
        reconnect();
    }

    private void initHandlers(){
        messageHandlers.add(new ConnectAckMessageHandler(this));
        messageHandlers.add(new SearchUserResultMessageHandler(this));
        messageHandlers.add(new GetUserInfoMessageHanlder(this));
        messageHandlers.add(new AddFriendRequestHandler(this));
        messageHandlers.add(new NotifyFriendHandler(this));
        messageHandlers.add(new FriendRequestHandler(this));
        messageHandlers.add(new HandlerFriendRequestHandler(this));
        messageHandlers.add(new FriendPullHandler(this));
        messageHandlers.add(new SendMessageHandler(this));
        messageHandlers.add(new ReceiveMessageHandler(this));
        messageHandlers.add(new NotifyMessageHandler(this));
        messageHandlers.add(new NotifyFriendRequestHandler(this));
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public String getUserName(){
        return userName;
    }

    public void setToken(String token){
        this.token = token;
    }


    @Override
    public void receiveMessage(Header header, ByteBufferList byteBufferList) {
         for(MessageHandler messageHandler : messageHandlers){
             if(messageHandler.match(header)){
                 messageHandler.processMessage(header,byteBufferList);
                 break;
             }
         }
    }

    @Override
    public void receiveException(Exception e) {
        log.e("comsince receive exception ",e);
        JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusUnconnected);
        removeHeartbeatScheduled();
        if(!userDisconnect){
            reconectScheduled = androidNIOClient.post(new Runnable() {
                @Override
                public void run() {
                    log.i("receiveException start reconnect");
                    reconnect();
                }
            }, 10 * 1000);
        }

    }

    @Override
    public void onConnected() {
        log.i("comsince connected");
       JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnected);
       removeReconnectScheduled();
       sendConnectMessage();
       heartbeatScheduled = androidNIOClient.post(new Runnable() {
           @Override
           public void run() {
               if(androidNIOClient.connectStatus == ConnectStatus.CONNECTED){
                   androidNIOClient.heart(60 * 1000);
               }
               heartbeatScheduled = androidNIOClient.post(this,50*1000);
           }
       },50 * 1000);
    }

//    public void reconnect(){
//        if(androidNIOClient != null){
//            androidNIOClient.post(new Runnable() {
//                @Override
//                public void run() {
//                    log.i("comsince reconnect");
//                    removeReconnectScheduled();
//                    if(androidNIOClient.connectStatus == ConnectStatus.DISCONNECT){
//                        removeHeartbeatScheduled();
//                        androidNIOClient.close();
//                    }
//                    reconnect0();
//                }
//            },0);
//        }
//
//    }

    public void reconnect(){
        if(!userDisconnect && androidNIOClient != null){
            androidNIOClient.post(new Runnable() {
                @Override
                public void run() {
                    log.i("reconnect status "+androidNIOClient.connectStatus);
                    switch (androidNIOClient.connectStatus){
                        case CONNECTING:
                            removeReconnectScheduled();
                            removeHeartbeatScheduled();
                            androidNIOClient.close();
                            JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnecting);
                            androidNIOClient.connect();
                            break;
                        case CONNECTED:
                            break;
                        case DISCONNECT:
                            removeReconnectScheduled();
                            removeHeartbeatScheduled();
                            JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnecting);
                            androidNIOClient.connect();
                            break;
                    }

                }
            },0);
        }
    }

//    public void reconnect0(){
//        if(!userDisconnect && androidNIOClient != null){
//            log.i("comsince start connect ");
//            if(androidNIOClient.connectStatus != ConnectStatus.CONNECTED){
//                JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnecting);
//            }
//            androidNIOClient.connect();
//        }
//    }

    private void removeHeartbeatScheduled(){
        if(heartbeatScheduled != null){
            androidNIOClient.removeScheduled(heartbeatScheduled);
        }
    }

    private void removeReconnectScheduled(){
        if(reconectScheduled != null){
            androidNIOClient.removeScheduled(reconectScheduled);
        }
    }

    private void sendConnectMessage(){
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setUserName(userName);
        byte[] byteToken = Base64.decode(token);
        byte[] aesToken = AES.AESDecrypt(byteToken,"",true);
        String allToken = new String(aesToken);

        String pwd = allToken.substring(0,allToken.indexOf("|"));
        allToken = allToken.substring(allToken.indexOf("|")+1);
        String secret = allToken.substring(0,allToken.indexOf("|"));
        log.i("pwd->"+pwd+ " secret-> "+secret);
        //利用secret加密pwd
        byte[] pwdAES = AES.AESEncrypt(Base64.decode(pwd),secret);
        log.i("base64 pwd encrypt->"+Base64.encode(pwdAES));
        connectMessage.setPassword(Base64.encode(pwdAES));
        connectMessage.setClientIdentifier(PreferenceManager.getDefaultSharedPreferences(context).getString("mars_core_uid", ""));
        log.i("send connectMessage "+JSON.toJSONString(connectMessage));
        sendMessage(Signal.CONNECT,SubSignal.NONE, JSON.toJSONString(connectMessage).getBytes(),null);
    }

    private void sendMessage(Signal signal,SubSignal subSignal,byte[] message,Object callback){
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                int messageId = PreferenceManager.getDefaultSharedPreferences(context).getInt("message_id",0);
                log.i("sendMessage send signal "+signal+" subSignal "+subSignal+" messageId "+messageId);
                androidNIOClient.sendMessage(signal, subSignal,messageId, message);
                if(callback != null){
                    RequestInfo requestInfo = new RequestInfo(signal,subSignal,callback.getClass(),callback);
                    requestMap.put(messageId,requestInfo);
                }
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("message_id",++messageId).apply();
            }
        });
    }

    private SimpleFuture sendMessageSync(Signal signal,SubSignal subSignal,byte[] message){
        SimpleFuture simpleFuture = new SimpleFuture();
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                int messageId = PreferenceManager.getDefaultSharedPreferences(context).getInt("message_id",0);
                log.i("sendMessageSync send signal "+signal+" subSignal "+subSignal+" messageId "+messageId);
                futureMap.put(messageId,simpleFuture);
                androidNIOClient.sendMessage(signal, subSignal,messageId, message);
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("message_id",++messageId).apply();
            }
        });
        return simpleFuture;
    }

    public void searchUser(String keyword, JavaProtoLogic.ISearchUserCallback callback){
        WFCMessage.SearchUserRequest request = WFCMessage.SearchUserRequest.newBuilder()
                .setKeyword(keyword)
                .setFuzzy(1)
                .setPage(0)
                .build();
        sendMessage(Signal.PUBLISH,SubSignal.US,request.toByteArray(),callback);
    }

    public void disConnect(int clearSession){
        userDisconnect = true;
        byte[] body = new byte[1];
        body[0] = (byte) clearSession;
        sendMessage(Signal.DISCONNECT,SubSignal.NONE,body,null);
    }

    public ProtoUserInfo getUserInfo(String userId, String groupId, boolean refresh){
        WFCMessage.PullUserRequest request = WFCMessage.PullUserRequest.newBuilder()
                .addRequest(WFCMessage.UserRequest.newBuilder().setUid(userId).build())
                .build();
        SimpleFuture<ProtoUserInfo> simpleFuture = sendMessageSync(Signal.PUBLISH,SubSignal.UPUI,request.toByteArray());
        try {
            return simpleFuture.get(200,TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.e("getuserinfo error ",e);
            return null;
        }
    }

    public void sendFriendRequest(String userId, String reason, JavaProtoLogic.IGeneralCallback callback){
        WFCMessage.AddFriendRequest friendRequest = WFCMessage.AddFriendRequest.newBuilder()
                .setReason(reason)
                .setTargetUid(userId)
                .build();
        sendMessage(Signal.PUBLISH, SubSignal.FAR,friendRequest.toByteArray(),callback);
    }

    public int getUnreadFriendRequestStatus(){
        ProtoFriendRequest[] protoFriendRequests = getFriendRequest(true);
        if(protoFriendRequests != null){
            return protoFriendRequests.length;
        } else {
            return 0;
        }
    }

    public ProtoFriendRequest[] getFriendRequest(boolean incomming) {
        WFCMessage.Version version = WFCMessage.Version.newBuilder().setVersion(System.currentTimeMillis() - 60 * 1000).build();
        SimpleFuture<ProtoFriendRequest[]> friendRequestFuture = sendMessageSync(Signal.PUBLISH,SubSignal.FRP,version.toByteArray());
        try {
            return friendRequestFuture.get(200,TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    public void handleFriendRequest(String userId, boolean accept, JavaProtoLogic.IGeneralCallback callback){
        WFCMessage.HandleFriendRequest handleFriendRequest = WFCMessage.HandleFriendRequest.newBuilder()
                .setTargetUid(userId)
                .setStatus(0)
                .build();
        sendMessage(Signal.PUBLISH,SubSignal.FHR,handleFriendRequest.toByteArray(),callback);
    }

    public String[] getMyFriendList(boolean refresh){
        if(!refresh && myFriendList != null){
            return myFriendList;
        }

        WFCMessage.Version request = WFCMessage.Version.newBuilder().setVersion(0).build();
        SimpleFuture<String[]> friendListFuture = sendMessageSync(Signal.PUBLISH,SubSignal.FP,request.toByteArray());
        try {
            myFriendList = friendListFuture.get(500,TimeUnit.MILLISECONDS);
            return myFriendList;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isMyFriend(String userId){
        if(myFriendList != null){
            for(String friend : myFriendList){
                if(friend.endsWith(userId)){
                    return true;
                }
            }
        }
        return false;
    }

    public long startMessageId = 0;
    public ProtoMessage[] getMessages(int conversationType, String target, int line, long fromIndex, boolean before, int count, String withUser){
        log.i("startMessageId "+startMessageId+"conversationType "+conversationType+" target "+target+" line "+line +" count "+" messageId "+startMessageId+" withuser "+withUser);
        WFCMessage.PullMessageRequest pullMessageRequest = WFCMessage.PullMessageRequest.newBuilder()
                .setId(startMessageId)
                .setType(conversationType)
                .build();
        SimpleFuture<ProtoMessage[]> pullMessageFuture = sendMessageSync(Signal.PUBLISH,SubSignal.MP,pullMessageRequest.toByteArray());
        try {
            ProtoMessage[] protoMessages = pullMessageFuture.get(500,TimeUnit.MILLISECONDS);
            List<ProtoMessage> protoMessageList = new ArrayList<>();
            for(ProtoMessage message: protoMessages){
                if(message.getTarget().equals(target)){
                    protoMessageList.add(message);
                }
            }
            ProtoMessage[] result = new ProtoMessage[protoMessageList.size()];
            protoMessageList.toArray(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ProtoMessage[0];
    }

    public void getRemoteMessages(int conversationType, String target, int line, long beforeMessageUid, int count, JavaProtoLogic.ILoadRemoteMessagesCallback callback){
        callback.onSuccess(new ProtoMessage[0]);
    }

    public void sendMessage(ProtoMessage msg, int expireDuration, JavaProtoLogic.ISendMessageCallback callback){
        WFCMessage.Message sendMessage = convertWFCMessage(msg);
        sendMessage(Signal.PUBLISH,SubSignal.MS,sendMessage.toByteArray(),callback);
    }


    public void pullMessage(long messageId,int type){
        WFCMessage.PullMessageRequest pullMessageRequest = WFCMessage.PullMessageRequest.newBuilder()
                .setId(messageId)
                .setType(type)
                .build();
        sendMessage(Signal.PUBLISH,SubSignal.MP,pullMessageRequest.toByteArray(),null);
    }




    public ProtoUserInfo convertUser(WFCMessage.User user){
        ProtoUserInfo protoUserInfo = new ProtoUserInfo();
        protoUserInfo.setUid(user.getUid());
        protoUserInfo.setEmail(user.getEmail());
        protoUserInfo.setDisplayName(user.getDisplayName());
        protoUserInfo.setMobile(user.getMobile());
        protoUserInfo.setName(user.getName());
        protoUserInfo.setAddress(user.getAddress());
        return protoUserInfo;
    }


    public ProtoMessage convertProtoMessage(WFCMessage.Message message){
        ProtoMessage messageResponse = new ProtoMessage();
        //log.i("current user "+userName +" fromuser "+message.getFromUser()+" target "+message.getToUser() +"message "+message.getContent().getSearchableContent());
        if(message.getFromUser().equals(userName)){
            messageResponse.setDirection(0);
        } else {
            messageResponse.setDirection(1);
        }
        messageResponse.setMessageId(message.getMessageId());
        messageResponse.setTimestamp(message.getServerTimestamp());
        WFCMessage.Conversation conversation  = message.getConversation();
        messageResponse.setConversationType(conversation.getType());
        if(!message.getFromUser().equals(userName)){
            messageResponse.setTarget(message.getFromUser());
            messageResponse.setFrom(message.getConversation().getTarget());
        } else {
            messageResponse.setTarget(message.getConversation().getTarget());
            messageResponse.setFrom(message.getFromUser());
        }

        messageResponse.setLine(conversation.getLine());
        WFCMessage.MessageContent messageContent = message.getContent();
        ProtoMessageContent messageConentResponse = new ProtoMessageContent();
        messageConentResponse.setType(messageContent.getType());
        messageConentResponse.setContent(messageContent.getContent());
        messageConentResponse.setPushContent(message.getContent().getPushContent());
        messageConentResponse.setSearchableContent(messageContent.getSearchableContent());
        messageResponse.setContent(messageConentResponse);
        return messageResponse;
    }

    public WFCMessage.Message convertWFCMessage(ProtoMessage messageResponse){
        WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
        ProtoService.log.i("fromuser "+messageResponse.getFrom()+" target "+messageResponse.getTarget() +
                " content type "+messageResponse.getContent().getType()+" tos "+messageResponse.getTos()+"direct "+messageResponse.getDirection()
                + "status "+messageResponse.getStatus());
        builder.setFromUser(messageResponse.getFrom());
        WFCMessage.Conversation conversation = WFCMessage.Conversation.newBuilder()
                .setType(messageResponse.getConversationType())
                .setLine(messageResponse.getLine())
                .setTarget(messageResponse.getTarget())
                .build();
        builder.setConversation(conversation);
        WFCMessage.MessageContent.Builder build = WFCMessage.MessageContent.newBuilder();
        build.setType(messageResponse.getContent().getType());
        if(!TextUtils.isEmpty(messageResponse.getContent().getSearchableContent())){
            build.setSearchableContent(messageResponse.getContent().getSearchableContent());
        }
        if(!TextUtils.isEmpty(messageResponse.getContent().getPushContent())){
            build.setPushContent(messageResponse.getContent().getPushContent());
        }
        builder.setContent(build.build());
        return builder.build();
    }

}
