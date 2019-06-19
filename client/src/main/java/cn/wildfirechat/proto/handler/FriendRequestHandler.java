package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class FriendRequestHandler extends AbstractMessagHandler{
    Log log = LoggerFactory.getLogger(FriendRequestHandler.class);
    public FriendRequestHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.FRP == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        if(errorCode == 0){
            try {
                WFCMessage.GetFriendRequestResult getFriendRequestResult = WFCMessage.GetFriendRequestResult.parseFrom(byteBufferList.getAllByteArray());
                ProtoFriendRequest[]  protoFriendRequests = new ProtoFriendRequest[getFriendRequestResult.getEntryCount()];
                for(int i = 0; i < protoFriendRequests.length ; i++){
                    protoFriendRequests[i] = convertProtoFriendRequest(getFriendRequestResult.getEntry(i));
                }
                log.i("process signal "+header.getSignal() +" subSignal "+header.getSubSignal()+"messageId "+header.getMessageId());
                protoService.futureMap.remove(header.getMessageId()).setComplete(protoFriendRequests);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    private ProtoFriendRequest convertProtoFriendRequest(WFCMessage.FriendRequest friendRequest){
        ProtoFriendRequest protoFriendRequest = new ProtoFriendRequest();
        protoFriendRequest.setTarget(friendRequest.getToUid());
        protoFriendRequest.setReason(friendRequest.getReason());
        protoFriendRequest.setStatus(friendRequest.getStatus());
        return protoFriendRequest;
    }


}
