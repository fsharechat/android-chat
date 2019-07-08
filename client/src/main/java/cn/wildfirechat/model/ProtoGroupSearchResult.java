package cn.wildfirechat.model;

/**
 * Created by taoli on 2017/12/4.
 */

public class ProtoGroupSearchResult {
    private ProtoGroupInfo groupInfo;

    /**
     命中的类型 0, 名字群组名字； 1，命中群成员名称；2，都命中
     */
    private int marchType;
    private String[] marchedMembers;

    public ProtoGroupInfo getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(ProtoGroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }

    public int getMarchType() {
        return marchType;
    }

    public void setMarchType(int marchType) {
        this.marchType = marchType;
    }

    public String[] getMarchedMembers() {
        return marchedMembers;
    }

    public void setMarchedMembers(String[] marchedMembers) {
        this.marchedMembers = marchedMembers;
    }
}
