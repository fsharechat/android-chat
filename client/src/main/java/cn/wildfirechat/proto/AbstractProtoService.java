package cn.wildfirechat.proto;

import android.content.Context;
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
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.alarm.AlarmWrapper;
import cn.wildfirechat.alarm.Timer;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.handler.MessageHandler;
import cn.wildfirechat.proto.handler.RequestInfo;
import cn.wildfirechat.proto.model.ConnectMessage;
import cn.wildfirechat.proto.model.QiuTokenMessage;
import cn.wildfirechat.proto.store.ImMemoryStore;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnected;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnecting;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusUnconnected;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Accept;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Accept_T;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_End;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Modify;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Signal;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Typing;

public abstract class AbstractProtoService implements PushMessageCallback {
    public static Log log = LoggerFactory.getLogger(ProtoService.class);
    private Context context;
    private String userName;
    private String token;
    private AndroidNIOClient androidNIOClient;
    protected ImMemoryStore imMemoryStore;
    UploadManager uploadManager;

    public ConcurrentHashMap<Integer, RequestInfo> requestMap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer, SimpleFuture> futureMap = new ConcurrentHashMap<>();
    protected List<MessageHandler> messageHandlers = new ArrayList<>();
    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private volatile boolean userDisconnect = false;
    private int reconnectNum = 0;
    private AlarmWrapper alarmWrapper;
    Timer heartbeatTimer;
    Timer reconnectTimer;
    int interval = 0;

    public AbstractProtoService(Context context,AlarmWrapper alarmWrapper){
        this.alarmWrapper = alarmWrapper;
        this.context = context;
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
        if(!userDisconnect && reconnectNum <= 3){
            log.i("reconnect num "+reconnectNum);
            reconnectNum++;
            alarmWrapper.schedule(reconnectTimer =
                    new Timer.Builder().period(reconnectNum * 10 * 1000)
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
        reconnectNum = 0;
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

    private void forceConnect(){
        log.i("force connect");
        if(!userDisconnect && androidNIOClient != null){
            androidNIOClient.post(new Runnable() {
                @Override
                public void run() {
                    cancelReconnectTimer();
                    cancelHeartTimer();
                    androidNIOClient.close();
                    JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnecting);
                    androidNIOClient.connect();
                }
            },0);
        }
    }

    private void schedule(){
        alarmWrapper.schedule(heartbeatTimer =
                new Timer.Builder().period((120 + 30 * interval) * 1000)
                        .wakeup(true)
                        .action(new Runnable() {
                            @Override
                            public void run() {
                                long current = (120 + 30 * ++interval) * 1000;
                                if(current > 8 * 60 * 1000){
                                    current = 8 * 60 * 1000;
                                }
                                sendHeartbeat(current);
                            }
                        }).build());
    }

    private void cancelHeartTimer(){
        interval--;
        if(interval < -2){
            interval = -2;
        }
        if(heartbeatTimer != null){
            alarmWrapper.cancel(heartbeatTimer);
        }
    }

    private void cancelReconnectTimer(){
        if(reconnectTimer != null){
            alarmWrapper.cancel(reconnectTimer);
        }
    }

    /**
     * 在应用在前台时尝试发送心跳指令
     * 在发送指令时需要先判断是否由定时任务尚未执行
     * */
    public void tryHeartbeat(){
        //interval参数不回退
        if(heartbeatTimer != null){
            alarmWrapper.cancel(heartbeatTimer);
        }
        long current = (120 + 30 * interval) * 1000;
        log.i("try send heartbeat interval "+current);
        sendHeartbeat(current);
    }

    private void sendHeartbeat(long interval){
        String heartInterval = "{\"interval\":" + interval + "}";
        sendMessage(Signal.PING, SubSignal.NONE, 1,heartInterval.getBytes(), new JavaProtoLogic.IGeneralCallback() {
            @Override
            public void onSuccess() {
                log.i("send heartbeat interval "+interval+" success");
                schedule();
            }

            @Override
            public void onFailure(int errorCode) {
                log.i("send heartbeat error");
            }
        });
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
        sendMessage(signal,subSignal,0,message,callback);
    }

    protected void sendMessage(Signal signal,SubSignal subSignal,long protoMessageId,byte[] message,Object callback){
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                //messageid最大不超过4898,以为私有协议messageId最大只支持65535
                int messageId = PreferenceManager.getDefaultSharedPreferences(context).getInt("message_id",0);
                if(messageId > 65535){
                    messageId = 0;
                }
                log.i("sendMessage send signal "+signal+" subSignal "+subSignal+" messageId "+messageId);
                androidNIOClient.sendMessage(signal, subSignal,messageId, message);
                if(callback != null){
                    RequestInfo requestInfo = new RequestInfo(signal,subSignal,callback.getClass(),callback);
                    requestInfo.setProtoMessageId(protoMessageId);
                    requestMap.put(messageId,requestInfo);
                }

                int finalMessageId = messageId;
                alarmWrapper.schedule(new Timer.Builder().period(3 * 1000)
                        .wakeup(true)
                        .action(new Runnable() {
                            @Override
                            public void run() {
                                RequestInfo requestInfo = requestMap.remove(finalMessageId);
                                if(requestInfo != null && requestInfo.getCallback() != null && protoMessageId != 0){
                                    log.i("send message timeout");
                                    try {
                                        Method onFailure = requestInfo.getType().getMethod("onFailure",int.class);
                                        onFailure.invoke(requestInfo.getCallback(),ErrorCode.SERVICE_DIED);
                                        imMemoryStore.updateMessageStatus(protoMessageId,MessageStatus.Send_Failure.ordinal());
                                        //如果发送失败，代表链接断开，需要重连
                                        forceConnect();
                                    } catch (NoSuchMethodException e) {
                                        e.printStackTrace();
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    } catch (InvocationTargetException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).build());

                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("message_id",++messageId).apply();

            }
        });
    }

    protected SimpleFuture sendMessageSync(Signal signal,SubSignal subSignal,byte[] message){
        SimpleFuture simpleFuture = new SimpleFuture();
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                int messageId = PreferenceManager.getDefaultSharedPreferences(context).getInt("message_id",0);
                if(messageId > 4898){
                    messageId = 0;
                }
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



    public void uploadMedia(byte[] data, int mediaType, JavaProtoLogic.IUploadMediaCallback callback){
        QiuTokenMessage qiuTokenMessage = getQiniuUploadToken(mediaType);
        if(qiuTokenMessage != null){
            String token = qiuTokenMessage.getToken();
            String domain = qiuTokenMessage.getDomain();
            String key = mediaType+"-"+getUserName()+"-"+System.currentTimeMillis();
            log.i("upload media type "+mediaType +" token "+token +" domain "+domain+" key "+key);

            uploadManager.put(data, key, token, new UpCompletionHandler() {
                @Override
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    log.i("key "+key+" info "+info );
                    if(info.statusCode == 200){
                        callback.onSuccess(domain+key);
                    } else {
                        callback.onFailure(ErrorCode.FILE_NOT_EXIST);
                    }
                }
            },null);
        } else {
            callback.onFailure(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    protected void uploadMedia(String data, int mediaType, JavaProtoLogic.IUploadMediaCallback callback){
        QiuTokenMessage qiuTokenMessage = getQiniuUploadToken(mediaType);
        if(qiuTokenMessage != null){
            String token = qiuTokenMessage.getToken();
            String domain = qiuTokenMessage.getDomain();
            String key = mediaType+"-"+getUserName()+"-"+System.currentTimeMillis()+"-"+data;
            log.i("upload media type "+mediaType +" token "+token +" domain "+domain+" key "+key);

            uploadManager.put(data, key, token, new UpCompletionHandler() {
                @Override
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    log.i("key "+key+" info "+info );
                    if(info.statusCode == 200){
                        callback.onSuccess(domain+key);
                    } else {
                        callback.onFailure(ErrorCode.FILE_NOT_EXIST);
                    }
                }
            },new UploadOptions(null, null, false, new UpProgressHandler() {
                @Override
                public void progress(String key, double percent) {
                    log.i("upload key "+key+" percent "+percent);
                    callback.onProgress((long) (100 * percent),100);
                }
            }, null));
        } else {
            callback.onFailure(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    private QiuTokenMessage getQiniuUploadToken(int mediaType){
        byte[] type = new byte[1];
        type[0] = (byte)mediaType;
        SimpleFuture<QiuTokenMessage> tokenFuture = sendMessageSync(Signal.PUBLISH,SubSignal.GQNUT,type);
        try {
            return tokenFuture.get(200, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
        log.i("receive current user "+userName +" fromuser "+message.getFromUser()+" target "+message.getToUser() +"message "+message.getContent().getSearchableContent()
        +"media type "+message.getContent().getMediaType()+" remote url "+message.getContent().getRemoteMediaUrl()
        +" messageId "+message.getMessageId()+" contentType "+message.getContent().getType()+" content "+message.getContent().getContent());
        if(message.getFromUser().equals(userName)){
            messageResponse.setDirection(0);
            messageResponse.setStatus(MessageStatus.Sent.value());
        } else {
            messageResponse.setDirection(1);
            messageResponse.setStatus(MessageStatus.Unread.value());
        }
        messageResponse.setMessageId(message.getMessageId());
        messageResponse.setMessageUid(message.getMessageId());
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
        if(messageContent.getData() != null){
            log.i("bintray content "+new String(messageContent.getData().toByteArray()));
        }
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
        log.i("send fromuser "+messageResponse.getFrom()+" target "+messageResponse.getTarget() +
                " content type "+messageResponse.getContent().getType()+" tos "+messageResponse.getTos()+"direct "+messageResponse.getDirection()
                + "status "+messageResponse.getStatus()+" messageUid "+messageResponse.getMessageUid() +" messageId "+messageResponse.getMessageId());
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
        log.i("localmediaurl "+messageResponse.getContent().getLocalMediaPath()
                +" remotemedieurl "+messageResponse.getContent().getRemoteMediaUrl()
               +" localcontent "+messageResponse.getContent().getLocalContent()
    +" mediaType "+messageResponse.getContent().getMediaType());

        if(isTransparentFlag(messageResponse.getContent().getType())){
            messageContentBuilder.setPersistFlag(ProtoConstants.PersistFlag.Transparent);
        }

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

    private boolean isTransparentFlag(int contentType){
        return contentType == ContentType_Typing ||
                contentType == ContentType_Call_Accept_T ||
                contentType == ContentType_Call_Signal ||
                contentType == ContentType_Call_End ||
                contentType == ContentType_Call_Modify ||
                contentType == ContentType_Call_Accept ;
    }
}
