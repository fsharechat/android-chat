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
import com.google.protobuf.ByteString;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import cn.wildfirechat.alarm.AlarmWrapper;
import cn.wildfirechat.alarm.Timer;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.handler.MessageHandler;
import cn.wildfirechat.proto.handler.RequestInfo;
import cn.wildfirechat.proto.model.ConnectMessage;
import cn.wildfirechat.proto.store.ImMemoryStore;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnected;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnecting;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusUnconnected;
import static com.tencent.mars.comm.PlatformComm.context;

public abstract class AbstractProtoService implements PushMessageCallback {
    public static Log log = LoggerFactory.getLogger(ProtoService.class);
    private String userName;
    private String token;
    private AndroidNIOClient androidNIOClient;
    protected ImMemoryStore imMemoryStore;


    public ConcurrentHashMap<Integer, RequestInfo> requestMap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer, SimpleFuture> futureMap = new ConcurrentHashMap<>();
    protected List<MessageHandler> messageHandlers = new ArrayList<>();
    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private volatile boolean userDisconnect = false;
    private AlarmWrapper alarmWrapper;
    Timer heartbeatTimer;
    Timer reconnectTimer;
    int interval = 0;

    public AbstractProtoService(AlarmWrapper alarmWrapper){
        this.alarmWrapper = alarmWrapper;
    }

    public void connect(String host, int shortPort){
        userDisconnect = false;
        androidNIOClient = new AndroidNIOClient(host,shortPort);
        androidNIOClient.setPushMessageCallback(this);
        reconnect();
    }

    public void stopProtoService(){
        if(androidNIOClient != null){
            androidNIOClient.setPushMessageCallback(null);
            androidNIOClient.close();
            androidNIOClient = null;
        }
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
        if(Signal.PING == header.getSignal()){
            schedule();
        }
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
        cancelHeartTimer();
        if(!userDisconnect){
            alarmWrapper.schedule(reconnectTimer =
                    new Timer.Builder().period(10 * 1000)
                            .wakeup(true)
                            .action(new Runnable() {
                                @Override
                                public void run() {
                                    log.i("receiveException start reconnect");
                                    reconnect();
                                }
                            }).build());
        }

    }

    @Override
    public void onConnected() {
        log.i("comsince connected");
        JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnected);
        cancelReconnectTimer();
        sendConnectMessage();
        schedule();
    }

    public void reconnect(){
        if(!userDisconnect && androidNIOClient != null){
            androidNIOClient.post(new Runnable() {
                @Override
                public void run() {
                    log.i("reconnect status "+androidNIOClient.connectStatus);
                    switch (androidNIOClient.connectStatus){
                        case CONNECTING:
                            cancelReconnectTimer();
                            cancelHeartTimer();
                            androidNIOClient.close();
                            JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnecting);
                            androidNIOClient.connect();
                            break;
                        case CONNECTED:
                            break;
                        case DISCONNECT:
                            cancelReconnectTimer();
                            cancelHeartTimer();
                            JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnecting);
                            androidNIOClient.connect();
                            break;
                    }

                }
            },0);
        }
    }

    private void schedule(){
        alarmWrapper.schedule(heartbeatTimer =
                new Timer.Builder().period((30 + 30 * interval) * 1000)
                        .wakeup(true)
                        .action(new Runnable() {
                            @Override
                            public void run() {
                                long current = (30 + 30 * ++interval) * 1000;
                                if(current > 8 * 60 * 1000){
                                    current = 8 * 60 * 1000;
                                }
                                androidNIOClient.heart(current);
                            }
                        }).build());
    }

    private void cancelHeartTimer(){
        interval = 0;
        if(heartbeatTimer != null){
            alarmWrapper.cancel(heartbeatTimer);
        }
    }

    private void cancelReconnectTimer(){
        if(reconnectTimer != null){
            alarmWrapper.cancel(reconnectTimer);
        }
    }

    private void sendConnectMessage(){
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setUserName(userName);
        byte[] byteToken = Base64.decode(token);
        byte[] aesToken = AES.AESDecrypt(byteToken,"",false);
        log.i("sendConnectMessage userName "+userName+" token "+token+" aesToken "+aesToken);
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
        log.i("send connectMessage "+ JSON.toJSONString(connectMessage));
        sendMessage(Signal.CONNECT, SubSignal.NONE, JSON.toJSONString(connectMessage).getBytes(),null);
    }

    protected void sendMessage(Signal signal,SubSignal subSignal,byte[] message,Object callback){
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


//    protected void sendMessage(Signal signal,SubSignal subSignal,byte[] message,String callbackParam,Object callback){
//        scheduledExecutorService.execute(new Runnable() {
//            @Override
//            public void run() {
//                int messageId = PreferenceManager.getDefaultSharedPreferences(context).getInt("message_id",0);
//                log.i("sendMessage send signal "+signal+" subSignal "+subSignal+" messageId "+messageId);
//                androidNIOClient.sendMessage(signal, subSignal,messageId, message);
//                if(callback != null){
//                    RequestInfo requestInfo = new RequestInfo(signal,subSignal,callback.getClass(),callback);
//                    if(!TextUtils.isEmpty(callbackParam)){
//                        requestInfo.setCallbackParam(callbackParam);
//                    }
//                    requestMap.put(messageId,requestInfo);
//                }
//                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("message_id",++messageId).apply();
//            }
//        });
//    }

    protected SimpleFuture sendMessageSync(Signal signal,SubSignal subSignal,byte[] message){
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



    public void disConnect(int clearSession){
        imMemoryStore.stop();
        cancelHeartTimer();
        cancelReconnectTimer();
        userDisconnect = true;
        byte[] body = new byte[1];
        body[0] = (byte) clearSession;
        sendMessage(Signal.DISCONNECT,SubSignal.NONE,body,null);
    }



    public ProtoUserInfo convertUser(WFCMessage.User user){
        ProtoUserInfo protoUserInfo = new ProtoUserInfo();
        protoUserInfo.setUid(user.getUid());
        protoUserInfo.setEmail(user.getEmail());
        protoUserInfo.setDisplayName(user.getDisplayName());
        protoUserInfo.setMobile(user.getMobile());
        protoUserInfo.setName(user.getName());
        protoUserInfo.setAddress(user.getAddress());
        protoUserInfo.setPortrait(user.getPortrait());
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

        messageResponse.setTarget(message.getConversation().getTarget());
        messageResponse.setFrom(message.getFromUser());
        if(conversation.getType() == ProtoConstants.ConversationType.ConversationType_Private){
            if(!message.getFromUser().equals(userName)){
                messageResponse.setTarget(message.getFromUser());
                messageResponse.setFrom(message.getFromUser());
            } else {
                messageResponse.setTarget(message.getConversation().getTarget());
                messageResponse.setFrom(message.getFromUser());
            }
        }


        messageResponse.setLine(conversation.getLine());
        WFCMessage.MessageContent messageContent = message.getContent();
        ProtoMessageContent messageContentResponse = new ProtoMessageContent();
        messageContentResponse.setType(messageContent.getType());
        messageContentResponse.setBinaryContent(messageContent.getData().toByteArray());
        messageContentResponse.setContent(messageContent.getContent());
        messageContentResponse.setPushContent(messageContent.getPushContent());
        messageContentResponse.setSearchableContent(messageContent.getSearchableContent());
        messageContentResponse.setMediaType(messageContent.getMediaType());
        messageContentResponse.setMentionedType(messageContent.getMentionedType());
        messageContentResponse.setRemoteMediaUrl(messageContent.getRemoteMediaUrl());
        messageResponse.setContent(messageContentResponse);
        return messageResponse;
    }

    public WFCMessage.Message convertWFCMessage(ProtoMessage messageResponse){
        WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
        log.i("fromuser "+messageResponse.getFrom()+" target "+messageResponse.getTarget() +
                " content type "+messageResponse.getContent().getType()+" tos "+messageResponse.getTos()+"direct "+messageResponse.getDirection()
                + "status "+messageResponse.getStatus());
        builder.setFromUser(messageResponse.getFrom());
        WFCMessage.Conversation conversation = WFCMessage.Conversation.newBuilder()
                .setType(messageResponse.getConversationType())
                .setLine(messageResponse.getLine())
                .setTarget(messageResponse.getTarget())
                .build();
        builder.setConversation(conversation);
        WFCMessage.MessageContent.Builder messageContentBuilder = WFCMessage.MessageContent.newBuilder();
        messageContentBuilder.setType(messageResponse.getContent().getType());
        if(!TextUtils.isEmpty(messageResponse.getContent().getSearchableContent())){
            messageContentBuilder.setSearchableContent(messageResponse.getContent().getSearchableContent());
        }
        if(!TextUtils.isEmpty(messageResponse.getContent().getPushContent())){
            messageContentBuilder.setPushContent(messageResponse.getContent().getPushContent());
        }
        if(messageResponse.getContent().getBinaryContent() != null){
            log.i("bintray content "+new String(messageResponse.getContent().getBinaryContent()));
            messageContentBuilder.setData(ByteString.copyFrom(messageResponse.getContent().getBinaryContent()));
        }
        if(!TextUtils.isEmpty(messageResponse.getContent().getContent())){
            messageContentBuilder.setContent(messageResponse.getContent().getContent());
        }
        messageContentBuilder.setMentionedType(messageResponse.getContent().getMentionedType());
        List<String> mentionTargets = new ArrayList<>();
        Collections.addAll(mentionTargets,messageResponse.getContent().getMentionedTargets());
        messageContentBuilder.addAllMentionedTarget(mentionTargets);

        messageContentBuilder.setMediaType(messageResponse.getContent().getMediaType());
        if(!TextUtils.isEmpty(messageResponse.getContent().getRemoteMediaUrl())){
            messageContentBuilder.setRemoteMediaUrl(messageResponse.getContent().getRemoteMediaUrl());
        }
        log.i("localurl "+messageResponse.getContent().getLocalMediaPath()
                +" remotemedieurl "+messageResponse.getContent().getRemoteMediaUrl()
               +" localcontent "+messageResponse.getContent().getLocalContent()
    +" mediaType "+messageResponse.getContent().getMediaType());


        builder.setContent(messageContentBuilder.build());
        return builder.build();
    }

    public WFCMessage.MessageContent convert2WfcMessageContent(ProtoMessageContent protoMessageContent){
        if(protoMessageContent != null){
            WFCMessage.MessageContent.Builder wfcMessageContentBuilder = WFCMessage.MessageContent.newBuilder();
            wfcMessageContentBuilder.setContent(protoMessageContent.getContent());
            wfcMessageContentBuilder.setType(protoMessageContent.getType());
            wfcMessageContentBuilder.setPushContent(protoMessageContent.getPushContent());
            wfcMessageContentBuilder.setMediaType(protoMessageContent.getMediaType());
            wfcMessageContentBuilder.setMentionedType(protoMessageContent.getMentionedType());
            wfcMessageContentBuilder.setSearchableContent(protoMessageContent.getSearchableContent());
            return wfcMessageContentBuilder.build();
        } else {
            return null;
        }


    }
}
