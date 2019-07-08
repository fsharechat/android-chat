package cn.wildfirechat.model;

/**
 * Created by taoli on 2017/12/4.
 */

public class ProtoConversationSearchresult {
    private int conversationType;
    private String target;
    private int line;
    //only marchedCount == 1, load the message
    private ProtoMessage marchedMessage;
    private long timestamp;
    private int marchedCount;

    public int getConversationType() {
        return conversationType;
    }

    public void setConversationType(int conversationType) {
        this.conversationType = conversationType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public ProtoMessage getMarchedMessage() {
        return marchedMessage;
    }

    public void setMarchedMessage(ProtoMessage marchedMessage) {
        this.marchedMessage = marchedMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getMarchedCount() {
        return marchedCount;
    }

    public void setMarchedCount(int marchedCount) {
        this.marchedCount = marchedCount;
    }
}
