package cn.wildfirechat.proto.handler;

import android.text.TextUtils;

import cn.wildfirechat.model.ProtoMessage;
import cn.wildfirechat.model.ProtoMessageContent;
import cn.wildfirechat.model.ProtoUserInfo;
import cn.wildfirechat.proto.ProtoService;
import cn.wildfirechat.proto.WFCMessage;

public abstract class AbstractMessagHandler implements MessageHandler{

    protected static String currentUserId;

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


    public static ProtoMessage convertProtoMessage(WFCMessage.Message message){
        ProtoMessage messageResponse = new ProtoMessage();
        if(currentUserId == message.getFromUser()){
            messageResponse.setDirection(0);
        } else {
            messageResponse.setDirection(1);
        }
        messageResponse.setMessageId(message.getMessageId());
        messageResponse.setTimestamp(message.getServerTimestamp());
        WFCMessage.Conversation conversation  = message.getConversation();
        messageResponse.setConversationType(conversation.getType());
        messageResponse.setTarget(message.getFromUser());
        messageResponse.setFrom(message.getConversation().getTarget());
        messageResponse.setLine(conversation.getLine());
        WFCMessage.MessageContent messageContent = message.getContent();
        ProtoMessageContent messageConentResponse = new ProtoMessageContent();
        messageConentResponse.setType(messageContent.getType());
        messageConentResponse.setPushContent(message.getContent().getPushContent());
        messageConentResponse.setSearchableContent(messageContent.getSearchableContent());
        messageResponse.setContent(messageConentResponse);
        return messageResponse;
    }

    public static WFCMessage.Message convertWFCMessage(ProtoMessage messageResponse){
        WFCMessage.Message.Builder builder = WFCMessage.Message.newBuilder();
        ProtoService.log.i("fromuser "+messageResponse.getFrom()+" target "+messageResponse.getTarget() +
                " content type "+messageResponse.getContent().getType()+" tos "+messageResponse.getTos()+"direct "+messageResponse.getDirection());
        builder.setFromUser(messageResponse.getFrom());
        WFCMessage.Conversation conversation = WFCMessage.Conversation.newBuilder()
                .setType(messageResponse.getConversationType())
                .setLine(messageResponse.getLine())
                .setTarget(messageResponse.getTarget())
                .build();
        builder.setConversation(conversation);
        WFCMessage.MessageContent.Builder build = WFCMessage.MessageContent.newBuilder();
        build.setType(messageResponse.getContent().getType());
        if(!TextUtils.isEmpty(messageResponse.getContent().getSearchableContent())){
            build.setSearchableContent(messageResponse.getContent().getSearchableContent());
        }
        if(!TextUtils.isEmpty(messageResponse.getContent().getPushContent())){
            build.setPushContent(messageResponse.getContent().getPushContent());
        }
        builder.setContent(build.build());
        return builder.build();
    }
}
