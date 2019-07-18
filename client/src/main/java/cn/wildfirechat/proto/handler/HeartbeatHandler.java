package cn.wildfirechat.proto.handler;

import android.text.TextUtils;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;

import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;

/**
 * 心跳处理器
 * */
public class HeartbeatHandler extends AbstractMessageHandler{

    public HeartbeatHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return Signal.PING == header.getSignal();
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        String pingResult = byteBufferList.readString();
        RequestInfo requestInfo = protoService.requestMap.remove(header.getMessageId());
        if(requestInfo != null){
            JavaProtoLogic.IGeneralCallback iGeneralCallback = (JavaProtoLogic.IGeneralCallback) requestInfo.getCallback();
            if(!TextUtils.isEmpty(pingResult)){
                logger.i("receive ping result: "+pingResult);
                iGeneralCallback.onSuccess();
            } else {
                iGeneralCallback.onFailure(ErrorCode.SERVICE_EXCEPTION);
            }
        }

    }
}
