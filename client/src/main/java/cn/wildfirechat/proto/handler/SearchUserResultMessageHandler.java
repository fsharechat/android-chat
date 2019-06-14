package cn.wildfirechat.proto.handler;

import com.comsince.github.core.ByteBufferList;
import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;
import com.comsince.github.push.Header;
import com.comsince.github.push.Signal;
import com.comsince.github.push.SubSignal;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.JavaProtoLogic;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public class SearchUserResultMessageHandler extends AbstractMessagHandler{
    private static final Log log  = LoggerFactory.getLogger(SearchUserResultMessageHandler.class);

    public SearchUserResultMessageHandler(ProtoService protoService) {
        super(protoService);
    }

    @Override
    public boolean match(Header header) {
        return header.getSignal() == Signal.PUB_ACK && header.getSubSignal() == SubSignal.US;
    }

    @Override
    public void processMessage(Header header, ByteBufferList byteBufferList) {
        int errorCode = byteBufferList.get();
        log.i("messageId "+header.getMessageId()+" "+protoService.requestMap.size());

        RequestInfo requestInfo = protoService.requestMap.remove(header.getMessageId());
        log.i("request Info "+requestInfo);
        try {
            if(requestInfo != null){
                WFCMessage.SearchUserResult userResult = WFCMessage.SearchUserResult.parseFrom(byteBufferList.getAllByteArray());
                log.i("receive error code "+errorCode+" user "+userResult.getEntryCount());
                ((JavaProtoLogic.ISearchUserCallback)requestInfo.getCallback()).onSuccess(protoUserInfos(userResult));

            }
        } catch (InvalidProtocolBufferException e) {
            log.e("error search result ",e);
            ((JavaProtoLogic.ISearchUserCallback)requestInfo.getCallback()).onFailure(errorCode);
        }
    }

    private ProtoUserInfo[] protoUserInfos(WFCMessage.SearchUserResult searchUserResult){
        ProtoUserInfo[] protoUserInfos = new ProtoUserInfo[searchUserResult.getEntryCount()];
        for(int i = 0;i<searchUserResult.getEntryCount();i++){
            WFCMessage.User user = searchUserResult.getEntry(i);
            log.i("user "+user.getDisplayName());
            protoUserInfos[i] = convertUser(user);
        }
        return protoUserInfos;
    }


}
