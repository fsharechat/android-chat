package cn.wildfirechat.proto.handler;

import android.text.TextUtils;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class ReceiveMessageHandler extends AbstractMessagHandler {

    private int currentMessageCount = 0;
    public static Map<String,ProtoMessage> protoMessageMap = new ConcurrentHashMap<>();
    public ReceiveMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.MP == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        ProtoService.log.i("receive message code "+errorCode);
        if(errorCode == 0){
            try {
                WFCMessage.PullMessageResult pullMessageResult = WFCMessage.PullMessageResult.parseFrom(byteBufferList.getAllByteArray());
                ProtoService.log.i("message count "+pullMessageResult.getMessageCount());
                ProtoMessage[] protoMessages = new ProtoMessage[pullMessageResult.getMessageCount()];
                for(int i = 0 ;i<pullMessageResult.getMessageCount();i++){
                    protoMessages[i] = protoService.convertProtoMessage(pullMessageResult.getMessage(i));


                    protoMessageMap.put(protoMessages[i].getTarget(),protoMessages[i]);
                    ProtoService.log.i("receive promessage "+protoMessages[i].getTarget());
                    if(!TextUtils.isEmpty(protoMessages[i].getContent().getPushContent())
                            || !TextUtils.isEmpty(protoMessages[i].getContent().getSearchableContent())
                            && protoService.futureMap.get(header.getMessageId()) == null){
                        if(JavaProtoLogic.unReadMessageIdMap.get(protoMessages[i].getTarget()) == null){
                            JavaProtoLogic.unReadMessageIdMap.put(protoMessages[i].getTarget(),protoMessages[i].getMessageId() -1);
                        }
                        currentMessageCount++;
                        if(currentMessageCount % 200 == 0){
                            if(JavaProtoLogic.unReadMessageIdMap.get(protoMessages[i].getTarget()) != null){
                                protoService.startMessageId = JavaProtoLogic.unReadMessageIdMap.get(protoMessages[i].getTarget());
                            } else {
                                protoService.startMessageId = protoMessages[i].getMessageId();
                            }
                        }

                        int unReadCount = 0;
                        if(JavaProtoLogic.unReadCountMap.get(protoMessages[i].getTarget()) != null){
                            unReadCount = JavaProtoLogic.unReadCountMap.get(protoMessages[i].getTarget());
                        }
                        JavaProtoLogic.unReadCountMap.put(protoMessages[i].getTarget(),++unReadCount);
                    }
                }
                SimpleFuture<ProtoMessage[]> pullMessageFutrue = protoService.futureMap.remove(header.getMessageId());
                if(pullMessageFutrue != null){
                    pullMessageFutrue.setComplete(protoMessages);
                } else {
                    workExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            JavaProtoLogic.onReceiveMessage(protoMessages,false);
                        }
                    });
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }
}
