package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;

import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

public class SendMessageHandler extends AbstractMessageHandler {

    public SendMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.MS == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
       int errorCode = byteBufferList.get();
       ProtoService.log.i("error code "+errorCode);
       RequestInfo requestInfo = protoService.requestMap.remove(header.getMessageId());
       if(requestInfo !=  null){
           JavaProtoLogic.ISendMessageCallback sendMessageCallback = (JavaProtoLogic.ISendMessageCallback) requestInfo.getCallback();
           if(errorCode == 0){
               long messageUid = byteBufferList.getLong();
               long timestamp = byteBufferList.getLong();
               boolean updateUid = protoService.getImMemoryStore().updateMessageUid(requestInfo.getProtoMessageId(),messageUid);
               boolean updateStatus = protoService.getImMemoryStore().updateMessageStatus(requestInfo.getProtoMessageId(), MessageStatus.Sent.ordinal());
               logger.i("messageUid "+messageUid+" timestamp "+timestamp+" updateUid "+updateUid+" updateStatus "+updateStatus);
               //有很多消息transparent消息在服务端和本地是不存储的，这些消息不进行seq计数
               if(updateUid && updateStatus){
                   protoService.getImMemoryStore().increaseMessageSeq();
               }
               sendMessageCallback.onSuccess(messageUid,timestamp);
           } else {
               sendMessageCallback.onFailure(errorCode);
               protoService.getImMemoryStore().updateMessageStatus(requestInfo.getProtoMessageId(), MessageStatus.Send_Failure.ordinal());
           }
       }
    }
}
