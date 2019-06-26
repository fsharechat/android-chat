package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.model.ProtoGroupInfo;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class GroupInfoHandler extends AbstractMessageHandler{

    public GroupInfoHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.GPGI == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        SimpleFuture<ProtoGroupInfo> protoGroupInfoSimpleFuture = protoService.futureMap.remove(header.getMessageId());
        if(errorCode == 0){
            try {
                WFCMessage.PullGroupInfoResult pullGroupInfoResult = WFCMessage.PullGroupInfoResult.parseFrom(byteBufferList.getAllByteArray());
                WFCMessage.GroupInfo groupInfo = pullGroupInfoResult.getInfo(0);
                ProtoGroupInfo protoGroupInfo = convert2ProGroupInfo(groupInfo);
                protoService.getImMemoryStore().addGroupInfo(groupInfo.getTargetId(),protoGroupInfo,true);
                protoGroupInfoSimpleFuture.setComplete(protoGroupInfo);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    public ProtoGroupInfo convert2ProGroupInfo(WFCMessage.GroupInfo wfcGroupInfo){
        ProtoGroupInfo protoGroupInfo = new ProtoGroupInfo();
        protoGroupInfo.setOwner(wfcGroupInfo.getOwner());
        protoGroupInfo.setTarget(wfcGroupInfo.getTargetId());
        protoGroupInfo.setName(wfcGroupInfo.getName());
        protoGroupInfo.setExtra(wfcGroupInfo.getExtra());
        protoGroupInfo.setMemberCount(wfcGroupInfo.getMemberCount());
        protoGroupInfo.setPortrait(wfcGroupInfo.getPortrait());
        protoGroupInfo.setType(wfcGroupInfo.getType());
        protoGroupInfo.setUpdateDt(wfcGroupInfo.getUpdateDt());
        return protoGroupInfo;
    }
}
