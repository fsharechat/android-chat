package cn.wildfirechat.proto.handler;

import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

abstract class AbstractMessagHandler implements MessageHandler{

    public AbstractMessagHandler(ProtoService protoService) {
        this.protoService = protoService;
    }

    ProtoService protoService;


    protected ProtoUserInfo convertUser(WFCMessage.User user){
        ProtoUserInfo protoUserInfo = new ProtoUserInfo();
        protoUserInfo.setUid(user.getUid());
        protoUserInfo.setEmail(user.getEmail());
        protoUserInfo.setDisplayName(user.getDisplayName());
        protoUserInfo.setMobile(user.getMobile());
        protoUserInfo.setName(user.getName());
        protoUserInfo.setAddress(user.getAddress());
        return protoUserInfo;
    }
}
