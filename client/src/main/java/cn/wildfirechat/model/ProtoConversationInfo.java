package cn.wildfirechat.model;

/**
 * Created by taoli on 2017/12/4.
 */

public class ProtoConversationInfo {
    private int conversationType;
    private String target;
    private int line;
    private ProtoMessage lastMessage;
    private long timestamp;
    private String draft;
    private ProtoUnreadCount unreadCount;
    private boolean isTop;
    private boolean isSilent;

    public boolean isSilent() {
        return isSilent;
    }

    public void setSilent(boolean silent) {
        isSilent = silent;
    }

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

    public ProtoMessage getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(ProtoMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }

    public ProtoUnreadCount getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(ProtoUnreadCount unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isTop() {
        return isTop;
    }

    public void setTop(boolean top) {
        isTop = top;
    }
}
