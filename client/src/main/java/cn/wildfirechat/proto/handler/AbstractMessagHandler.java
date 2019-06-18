package cn.wildfirechat.proto.handler;

import cn.wildfirechat.proto.ProtoService;

public abstract class AbstractMessagHandler implements MessageHandler{

    public AbstractMessagHandler(ProtoService protoService) {
        this.protoService = protoService;
    }

    ProtoService protoService;



}
