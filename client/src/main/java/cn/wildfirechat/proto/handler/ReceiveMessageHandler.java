package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;


/**
 * 用于处理pull消息的handler，消息发送流程如下：
 * --通知拉取方式
 * 1.服务端首先发送MN信令通知用户需要拉去消息
 * 2.用户发送MP信令拉去消息，拉去消息逻辑时根据消息序列号递增拉去，有可能一段时间内出现消息重复的现象
 * 为了解决消息重复拉去的问题
 * 采用直接发送推送信令，不会出现重复拉取的问题
 * --主动拉取消息拉取
 * 用户登录完成时，主动发起MP信令拉去消息，这类消息不是push类型，属于用户主动发起的消息拉取行为
 * */
public class ReceiveMessageHandler extends AbstractMessageHandler {

    //专门处理下拉消息，保证消息处理严格有序
    public Executor workMessageExecutor = Executors.newFixedThreadPool(1);

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
                workMessageExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        long head = pullMessageResult.getHead();
                        logger.i("message count "+pullMessageResult.getMessageCount()+" head "+head);
                        protoService.getImMemoryStore().updateMessageSeq(head);

                        boolean isUserPull = false;
                        RequestInfo requestInfo = protoService.requestMap.remove(header.getMessageId());
                        if(requestInfo != null && requestInfo.getCallback() != null){
                            isUserPull = true;
                        }
                        if(isUserPull){
                            processUserPullMessages(pullMessageResult.getMessageList(),requestInfo);
                        } else {
                            processNotifyPullMessage(pullMessageResult.getMessageList());
                        }
                    }
                });
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 处理用户主动拉取的消息
     * */
    private void processUserPullMessages(List<WFCMessage.Message> wfcMessageList,RequestInfo requestInfo){
        Map<String,List<ProtoMessage>> protoMessageMap = new HashMap<>();
        for(WFCMessage.Message wfcMessage : wfcMessageList){
            ProtoMessage protoMessage = protoService.convertProtoMessage(wfcMessage);
            logger.i("current message target "+protoMessage.getTarget()+" contentType "+protoMessage.getContent().getType());

            if(canPersistent(protoMessage.getContent().getType())){
                String target = protoMessage.getTarget();
                List<ProtoMessage> protoMessages = protoMessageMap.get(target);
                if(protoMessages == null){
                    protoMessages = new ArrayList<>();
                }
                protoMessages.add(protoMessage);
                protoMessageMap.put(target,protoMessages);
            }
        }
        List<ProtoMessage> conversationProtoMessage = new ArrayList<>();
        for(Map.Entry<String,List<ProtoMessage>> entry : protoMessageMap.entrySet()){
            String target = entry.getKey();
            logger.i("current conversation target "+target);
            List<ProtoMessage> addProtoMessages = entry.getValue();
            conversationProtoMessage.add(addProtoMessages.get(addProtoMessages.size() - 1));
            protoService.getImMemoryStore().addProtoMessagesByTarget(target,addProtoMessages,false);
        }
        //通知会话显示
        ProtoMessage[] conversationProtoMessageArr = new ProtoMessage[conversationProtoMessage.size()];
        conversationProtoMessage.toArray(conversationProtoMessageArr);
        JavaProtoLogic.onReceiveMessage(conversationProtoMessageArr,false);

        JavaProtoLogic.IGeneralCallback  iGeneralCallback = (JavaProtoLogic.IGeneralCallback) requestInfo.getCallback();
        iGeneralCallback.onSuccess();
    }

    private void processNotifyPullMessage(List<WFCMessage.Message> wfcMessageList){
        List<ProtoMessage> resultProtoMessageList = new ArrayList<>();
        for(WFCMessage.Message wfcMessage : wfcMessageList){
            logger.i("messageType "+wfcMessage.getContent().getType());
            ProtoMessage protoMessage = protoService.convertProtoMessage(wfcMessage);
            //为什么要去重，因为语音通话消息信令发送频繁，容易出现消息重复，可能影响通话流程
            //这里的消息最好在一个线程里面进行，不然插入数据还没生效，可能出现幻读，否则会出现信令重复，可能出现意想不到的结果
            ProtoMessage existProtoMessage = protoService.getImMemoryStore().getMessage(protoMessage.getMessageId());
            if(existProtoMessage == null) {
                resultProtoMessageList.add(protoMessage);
            }
            //所有透传的消息都要发送给目标，只是存储的时候需要过滤
            if(canPersistent(protoMessage.getContent().getType())){
                protoService.getImMemoryStore().addProtoMessageByTarget(protoMessage.getTarget(),protoMessage,true);
            }
        }
        ProtoMessage[] protoMessagesArr = new ProtoMessage[resultProtoMessageList.size()];
        resultProtoMessageList.toArray(protoMessagesArr);
        JavaProtoLogic.onReceiveMessage(protoMessagesArr,false);
    }
}
