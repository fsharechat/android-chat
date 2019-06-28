package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.proto.model.QiuTokenMessage;

public class QiniuTokenHandler extends AbstractMessageHandler {

    public QiniuTokenHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.GQNUT == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        SimpleFuture<QiuTokenMessage> tokenMessageSimpleFuture = protoService.futureMap.remove(header.getMessageId());
        if(errorCode == 0){
            try {
                WFCMessage.GetUploadTokenResult result = WFCMessage.GetUploadTokenResult.parseFrom(byteBufferList.getAllByteArray());
                QiuTokenMessage qiuTokenMessage = new QiuTokenMessage();
                qiuTokenMessage.setDomain(result.getDomain());
                qiuTokenMessage.setToken(result.getToken());
                tokenMessageSimpleFuture.setComplete(qiuTokenMessage);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }
}
