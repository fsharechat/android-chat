package cn.wildfirechat.proto.handler;

import android.text.TextUtils;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

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
        ProtoService.log.i("receive message code "+errorCode);
        if(errorCode == 0){
            try {
                WFCMessage.PullMessageResult pullMessageResult = WFCMessage.PullMessageResult.parseFrom(byteBufferList.getAllByteArray());
                ProtoService.log.i("message count "+pullMessageResult.getMessageCount());
                ProtoMessage[] protoMessages = new ProtoMessage[pullMessageResult.getMessageCount()];
                for(int i = 0 ;i<pullMessageResult.getMessageCount();i++){
                    logger.i("messsageType "+pullMessageResult.getMessage(i).getContent().getType());
                    ProtoMessage protoMessage = protoService.convertProtoMessage(pullMessageResult.getMessage(i));
                    protoService.getImMemoryStore().increaseMessageSeq();
                    if(protoMessage.getContent().getType() < 10){
                        protoMessages[i] = protoMessage;
                        protoService.getImMemoryStore().addProtoMessageByTarget(protoMessages[i].getTarget(),protoMessages[i],protoService.futureMap.get(header.getMessageId()) == null);
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
