package cn.wildfirechat.model;

import java.io.Serializable;
import java.util.List;

public class ProtoChatRoomMembersInfo implements Serializable {
    private int memberCount;
    private List<String> members;

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
