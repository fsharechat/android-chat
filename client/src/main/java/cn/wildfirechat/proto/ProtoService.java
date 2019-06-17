package cn.wildfirechat.proto;

import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.comsince.github.client.AndroidNIOClient;
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

    private String[] myFriendList;

    public ProtoService(){
        initHandlers();
    }

    public void connect(String host, int shortPort){
        androidNIOClient = new AndroidNIOClient(host,shortPort);
        androidNIOClient.setPushMessageCallback(this);
        androidNIOClient.connect();
    }

    private void initHandlers(){
        messageHandlers.add(new ConnectAckMessageHandler());
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
    }

    public void setUserName(String userName){
        this.userName = userName;
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
        JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusUnconnected);
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnecting);
                androidNIOClient.connect();
            }
        },10,TimeUnit.SECONDS);
    }

    @Override
    public void onConnected() {
       JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnected);
       sendConnectMessage();
       scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
           @Override
           public void run() {
                androidNIOClient.heart(50*1000);
           }
       },10,30, TimeUnit.SECONDS);
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
                log.i("send message id "+messageId + callback);
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
                androidNIOClient.sendMessage(signal, subSignal,messageId, message);
                futureMap.put(messageId,simpleFuture);
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
        byte[] body = new byte[1];
        body[0] = (byte) clearSession;
        sendMessage(Signal.DISCONNECT,SubSignal.NONE,body,null);
        scheduledExecutorService.shutdown();
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
        WFCMessage.Version version = WFCMessage.Version.newBuilder().setVersion(System.currentTimeMillis() - 10 * 60 * 1000).build();
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
        log.i("conversationType "+conversationType+" target "+target+" line "+line +" fromIndex "+fromIndex +" before "+before +" count "+" messageId "+startMessageId);
        WFCMessage.PullMessageRequest pullMessageRequest = WFCMessage.PullMessageRequest.newBuilder()
                .setId(startMessageId)
                .setType(conversationType)
                .build();
        SimpleFuture<ProtoMessage[]> pullMessageFuture = sendMessageSync(Signal.PUBLISH,SubSignal.MP,pullMessageRequest.toByteArray());
        try {
            return pullMessageFuture.get(500,TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ProtoMessage[0];
    }

    public void getRemoteMessages(int conversationType, String target, int line, long beforeMessageUid, int count, JavaProtoLogic.ILoadRemoteMessagesCallback callback){
        callback.onSuccess(new ProtoMessage[0]);
    }

    public void sendMessage(ProtoMessage msg, int expireDuration, JavaProtoLogic.ISendMessageCallback callback){
        WFCMessage.Message sendMessage = AbstractMessagHandler.convertWFCMessage(msg);
        sendMessage(Signal.PUBLISH,SubSignal.MS,sendMessage.toByteArray(),callback);
    }


    public void pullMessage(long messageId,int type){
        WFCMessage.PullMessageRequest pullMessageRequest = WFCMessage.PullMessageRequest.newBuilder()
                .setId(messageId)
                .setType(type)
                .build();
        sendMessage(Signal.PUBLISH,SubSignal.MP,pullMessageRequest.toByteArray(),null);
    }


}
