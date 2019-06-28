package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.model.ProtoFriendRequest;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class FriendRequestHandler extends AbstractMessageHandler {
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
                List<ProtoFriendRequest> protoFriendRequestList = new ArrayList<>();

                for(int i = 0; i < getFriendRequestResult.getEntryCount() ; i++){
                    ProtoFriendRequest protoFriendRequest = convertProtoFriendRequest(getFriendRequestResult.getEntry(i));
                    log.i("target "+protoFriendRequest.getTarget()+" reason "+protoFriendRequest.getReason()+" status "+protoFriendRequest.getStatus());
                    if(!protoService.getUserName().equals(protoFriendRequest.getTarget())){
                        protoFriendRequestList.add(protoFriendRequest);
                        protoService.getImMemoryStore().addProtoFriendRequest(protoFriendRequest);
                    }
                }
                ProtoFriendRequest[] protoFriendRequests = new ProtoFriendRequest[protoFriendRequestList.size()];
                protoFriendRequestList.toArray(protoFriendRequests);

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
