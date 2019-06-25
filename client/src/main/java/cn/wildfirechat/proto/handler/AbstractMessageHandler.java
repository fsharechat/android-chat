package cn.wildfirechat.proto.handler;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import cn.wildfirechat.proto.ProtoService;

public abstract class AbstractMessageHandler implements MessageHandler{

    Log logger = LoggerFactory.getLogger(AbstractMessageHandler.class);

    public AbstractMessageHandler(ProtoService protoService) {
        this.protoService = protoService;
    }

    ProtoService protoService;

    public Executor workExecutor = Executors.newFixedThreadPool(1);

}
