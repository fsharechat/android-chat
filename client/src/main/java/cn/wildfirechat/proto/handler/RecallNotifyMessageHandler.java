package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class RecallNotifyMessageHandler extends AbstractMessageHandler{

    public RecallNotifyMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUBLISH == header.getSignal() && SubSignal.RMN == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        try {
            WFCMessage.NotifyRecallMessage notifyRecallMessage = WFCMessage.NotifyRecallMessage.parseFrom(byteBufferList.getAllByteArray());
            long messageUid = notifyRecallMessage.getId();
            String operatorId = notifyRecallMessage.getFromUser();
            logger.i("receive operator "+operatorId+" recall message uid "+messageUid);
            workExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    ProtoMessage updateProtoMessage = protoService.getMessageByUid(messageUid);
                    RecallMessageContent recallMessageContent = new RecallMessageContent(operatorId, messageUid);
                    MessagePayload payload = recallMessageContent.encode();
                    payload.contentType = recallMessageContent.getClass().getAnnotation(ContentTag.class).type();
                    updateProtoMessage.setContent(payload.toProtoContent());
                    protoService.updateMessageContent(updateProtoMessage);
                    JavaProtoLogic.onRecallMessage(messageUid);

                }
            });
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
