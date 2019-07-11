package cn.wildfirechat.proto.handler;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.proto.ProtoService;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Accept;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Accept_T;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_End;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Modify;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Signal;

public abstract class AbstractMessageHandler implements MessageHandler{

    Log logger = LoggerFactory.getLogger(AbstractMessageHandler.class);

    public AbstractMessageHandler(ProtoService protoService) {
        this.protoService = protoService;
    }

    ProtoService protoService;

    public Executor workExecutor = Executors.newFixedThreadPool(3);

    protected Object getCallback(int messageId){
        return protoService.requestMap.remove(messageId).getCallback();
    }

    /**
     * 注意这里屏蔽了通话相关的指令，这些消息时不存储到数据中
     * */
    protected boolean canPersistent(int contentType){
        return contentType != MessageContentType.ContentType_Typing &&
                contentType != ContentType_Call_End &&
                contentType != ContentType_Call_Accept &&
                contentType != ContentType_Call_Signal &&
                contentType != ContentType_Call_Modify &&
                contentType != ContentType_Call_Accept_T
                ;
    }

}
