package cn.wildfirechat.proto.handler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.wildfirechat.proto.ProtoService;

public abstract class AbstractMessagHandler implements MessageHandler{

    public AbstractMessagHandler(ProtoService protoService) {
        this.protoService = protoService;
    }

    ProtoService protoService;

    public Executor workExecutor = Executors.newFixedThreadPool(1);

}
