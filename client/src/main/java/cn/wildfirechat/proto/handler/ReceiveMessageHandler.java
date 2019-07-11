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

import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class ReceiveMessageHandler extends AbstractMessageHandler {

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
        logger.i("receive message code "+errorCode +" remain "+" body count "+header.getLength()+" "+byteBufferList.remaining());
        if(errorCode == 0){
            try {
                WFCMessage.PullMessageResult pullMessageResult = WFCMessage.PullMessageResult.parseFrom(byteBufferList.getAllByteArray());
                workExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        long head = pullMessageResult.getHead();
                        logger.i("message count "+pullMessageResult.getMessageCount()+" head "+head);
                        protoService.getImMemoryStore().updateMessageSeq(head);
                        List<ProtoMessage> resultProtoMessageList = new ArrayList<>();
                        Map<String,List<ProtoMessage>> targetProtoMessageMap = new HashMap<>();
                        for(WFCMessage.Message wfcMessage : pullMessageResult.getMessageList()){
                            logger.i("messageType "+wfcMessage.getContent().getType());
                            ProtoMessage protoMessage = protoService.convertProtoMessage(wfcMessage);
                            if(!TextUtils.isEmpty(protoMessage.getTarget())){
                                if(canPersistent(protoMessage.getContent().getType())){
                                    List<ProtoMessage> protoMessages = targetProtoMessageMap.get(protoMessage.getTarget());
                                    if(protoMessages == null){
                                        protoMessages = new ArrayList<>();
                                    }
                                    protoMessages.add(protoMessage);
                                    targetProtoMessageMap.put(protoMessage.getTarget(),protoMessages);
                                }
                            }
                        }
                        boolean isPush = protoService.futureMap.get(header.getMessageId()) == null;
                        for(Map.Entry<String,List<ProtoMessage>> entry : targetProtoMessageMap.entrySet()){
                            String target = entry.getKey();
                            List<ProtoMessage> protoMessageList = entry.getValue();
                            logger.i("target "+target+" size "+protoMessageList.size());
                            protoService.getImMemoryStore().addProtoMessagesByTarget(target,protoMessageList,isPush);
                            resultProtoMessageList.addAll(protoMessageList);
                        }
                        ProtoMessage[] protoMessagesArr = new ProtoMessage[resultProtoMessageList.size()];
                        resultProtoMessageList.toArray(protoMessagesArr);
                        SimpleFuture<ProtoMessage[]> pullMessageFuture = protoService.futureMap.remove(header.getMessageId());
                        if(!isPush){
                            pullMessageFuture.setComplete(protoMessagesArr);
                        } else {
                            JavaProtoLogic.onReceiveMessage(protoMessagesArr,false);
                        }
                    }
                });
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }
}
