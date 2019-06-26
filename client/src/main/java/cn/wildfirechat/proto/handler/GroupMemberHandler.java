package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.core.future.SimpleFuture;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.model.ProtoGroupMember;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class GroupMemberHandler extends AbstractMessageHandler{

    public GroupMemberHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PUB_ACK == header.getSignal() && SubSignal.GPGM == header.getSubSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        SimpleFuture<ProtoGroupMember[]> groupMemberFuture = protoService.futureMap.remove(header.getMessageId());
        if(errorCode == 0){
            try {
                WFCMessage.PullGroupMemberResult pullGroupMemberResult = WFCMessage.PullGroupMemberResult.parseFrom(byteBufferList.getAllByteArray());
                ProtoGroupMember[] protoGroupMembers = new ProtoGroupMember[pullGroupMemberResult.getMemberCount()];
                List<ProtoGroupMember> protoGroupMemberList = new ArrayList<>();
                for(WFCMessage.GroupMember groupMember : pullGroupMemberResult.getMemberList()){
                    ProtoGroupMember protoGroupMember = convert2GroupMember(groupMember);
                    protoGroupMemberList.add(protoGroupMember);
                }
                protoGroupMemberList.toArray(protoGroupMembers);
                groupMemberFuture.setComplete(protoGroupMembers);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    private ProtoGroupMember convert2GroupMember(WFCMessage.GroupMember wfcGroupMember){
        ProtoGroupMember protoGroupMember = new ProtoGroupMember();
        protoGroupMember.setAlias(wfcGroupMember.getAlias());
        protoGroupMember.setMemberId(wfcGroupMember.getMemberId());
        protoGroupMember.setType(wfcGroupMember.getType());
        protoGroupMember.setUpdateDt(wfcGroupMember.getUpdateDt());
        return protoGroupMember;
    }
}
