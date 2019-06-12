package cn.wildfirechat.proto;

import android.preference.PreferenceManager;

import com.alibaba.fastjson.JSON;
import com.comsince.github.client.AndroidNIOClient;
import com.comsince.github.client.PushMessageCallback;
import com.comsince.github.core.callback.CompletedCallback;
import com.comsince.github.push.Signal;

import cn.wildfirechat.proto.model.ConnectMessage;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnected;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusUnconnected;
import static com.tencent.mars.comm.PlatformComm.context;

public class ProtoService implements PushMessageCallback {
    private String userName;
    private String token;
    private AndroidNIOClient androidNIOClient;

    public void connect(String host, int shortPort){
        androidNIOClient = new AndroidNIOClient(host,shortPort);
        androidNIOClient.setPushMessageCallback(this);
        androidNIOClient.connect();
    }

    public void setUserName(String userName){
        this.userName = userName;
    }

    public void setToken(String token){
        this.token = token;
    }

    @Override
    public void receiveMessage(Signal signal, String s) {

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
        connectMessage.setPassword(token);
        connectMessage.setClientIdentifier(PreferenceManager.getDefaultSharedPreferences(context).getString("mars_core_uid", ""));
        androidNIOClient.sendMessage(Signal.CONNECT, JSON.toJSONString(connectMessage), new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
            }
        });
    }
}
