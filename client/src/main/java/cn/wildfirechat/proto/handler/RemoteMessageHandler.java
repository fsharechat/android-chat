package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class RemoteMessageHandler extends AbstractMessageHandler{

    public RemoteMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.LRM == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        RequestInfo requestInfo = protoService.requestMap.remove(header.getMessageId());
        JavaProtoLogic.ILoadRemoteMessagesCallback iLoadRemoteMessagesCallback = (JavaProtoLogic.ILoadRemoteMessagesCallback) requestInfo.getCallback();
        if(errorCode == 0){
            try {
                WFCMessage.PullMessageResult pullMessageResult = WFCMessage.PullMessageResult.parseFrom(byteBufferList.getAllByteArray());
                int size = pullMessageResult.getMessageCount();
                List<ProtoMessage> protoMessageList = new ArrayList<>();
                //服务端返回的是降序排列，需要颠倒一下
                for(int i = size -1 ; i >= 0; i--){
                    WFCMessage.Message wfcMessage = pullMessageResult.getMessage(i);
                    if(canPersistent(wfcMessage.getContent().getType())){
                        ProtoMessage protoMessage = protoService.convertProtoMessage(wfcMessage);
                        protoMessageList.add(protoMessage);
                    }
                }
                ProtoMessage[] protoMessages = new ProtoMessage[protoMessageList.size()];
                protoMessageList.toArray(protoMessages);
                iLoadRemoteMessagesCallback.onSuccess(protoMessages);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else {
            iLoadRemoteMessagesCallback.onFailure(errorCode);
        }
    }
}
