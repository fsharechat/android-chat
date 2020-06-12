package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.proto.FSCMessage;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.model.MinioMessage;

public class GetMinioUploadUrlHandler extends AbstractMessageHandler{

    public GetMinioUploadUrlHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return header.getSignal() == Signal.PUB_ACK && header.getSubSignal() == SubSignal.GMURL;
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        SimpleFuture<MinioMessage> minioMessageSimpleFuture = protoService.futureMap.remove(header.getMessageId());
        if(errorCode == 0){
            try {
                FSCMessage.GetMinioUploadUrlResult result = FSCMessage.GetMinioUploadUrlResult.parseFrom(byteBufferList.getAllByteArray());
                MinioMessage minioMessage = new MinioMessage();
                minioMessage.setDomain(result.getDomain());
                minioMessage.setUrl(result.getUrl());
                logger.i("domain "+minioMessage.getDomain()+" url "+minioMessage.getUrl());
                minioMessageSimpleFuture.setComplete(minioMessage);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }
}
