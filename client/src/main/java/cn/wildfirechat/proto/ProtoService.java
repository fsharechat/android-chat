package cn.wildfirechat.proto;

import android.preference.PreferenceManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.comsince.github.client.AndroidNIOClient;
import com.comsince.github.client.PushMessageCallback;
import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.callback.CompletedCallback;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.comsince.github.push.util.AES;
import com.comsince.github.push.util.Base64;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.proto.handler.ConnectAckMessageHandler;
import cn.wildfirechat.proto.handler.MessageHandler;
import cn.wildfirechat.proto.model.ConnectMessage;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnected;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusUnconnected;
import static com.tencent.mars.comm.PlatformComm.context;

public class ProtoService implements PushMessageCallback {
    private static String TAG = "ProtoService";
    private String userName;
    private String token;
    private AndroidNIOClient androidNIOClient;

    private List<MessageHandler> messageHandlers = new ArrayList<>();

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
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public void setToken(String token){
        this.token = token;
    }


    @Override
    public void receiveMessage(Signal signal, SubSignal subSignal, ByteBufferList byteBufferList) {
         for(MessageHandler messageHandler : messageHandlers){
             if(messageHandler.match(signal)){
                 messageHandler.processMessage(signal,subSignal,byteBufferList);
                 break;
             }
         }
    }

    @Override
    public void receiveException(Exception e) {
        JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusUnconnected);
    }

    @Override
    public void onConnected() {
       JavaProtoLogic.onConnectionStatusChanged(ConnectionStatusConnected);
       sendConnectMessage();
    }

    private void sendConnectMessage(){
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setUserName(userName);
        byte[] byteToken = Base64.decode(token);
        byte[] aesToken = AES.AESDecrypt(byteToken,"",true);
        String allToken = new String(aesToken);

        Log.i(TAG,"all token is "+allToken);

        String pwd = allToken.substring(0,allToken.indexOf("|"));
        allToken = allToken.substring(allToken.indexOf("|")+1);
        String secret = allToken.substring(0,allToken.indexOf("|"));
        Log.i(TAG,"pwd->"+pwd+ " secret-> "+secret);
        //利用secret加密pwd
        byte[] pwdAES = AES.AESEncrypt(Base64.decode(pwd),secret);
        Log.i(TAG,"base64 pwd encrypt->"+Base64.encode(pwdAES));
        connectMessage.setPassword(Base64.encode(pwdAES));
        connectMessage.setClientIdentifier(PreferenceManager.getDefaultSharedPreferences(context).getString("mars_core_uid", ""));
        Log.i(TAG,"send connectMessage "+JSON.toJSONString(connectMessage));
        androidNIOClient.sendMessage(Signal.CONNECT, JSON.toJSONString(connectMessage), new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
            }
        });
    }
}
