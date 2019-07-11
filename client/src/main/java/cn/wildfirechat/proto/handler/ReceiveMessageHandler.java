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
                        boolean isPush = protoService.futureMap.get(header.getMessageId()) == null;
                        for(WFCMessage.Message wfcMessage : pullMessageResult.getMessageList()){
                            logger.i("messageType "+wfcMessage.getContent().getType());
                            ProtoMessage protoMessage = protoService.convertProtoMessage(wfcMessage);
                            //为什么要去重，因为语音通话消息信令发送频繁，容易出现消息重复，可能影响通话流程
                            //这里的消息最好在一个线程里面进行，不然插入数据还没生效，可能出现幻读，否则会出现信令重复，可能出现意想不到的结果
                            ProtoMessage existProtoMessage = protoService.getImMemoryStore().getMessage(protoMessage.getMessageId());
                            if(existProtoMessage == null) {
                                resultProtoMessageList.add(protoMessage);
                                protoService.getImMemoryStore().addProtoMessageByTarget(protoMessage.getTarget(),protoMessage,isPush);
                            }
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
