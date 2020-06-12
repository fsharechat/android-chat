package cn.wildfirechat.proto.model;

import android.os.Parcel;
import android.os.Parcelable;

public class MinioMessage implements Parcelable {
    String domain;
    String url;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MinioMessage(){

    }

    protected MinioMessage(Parcel in) {
        domain = in.readString();
        url = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(domain);
        dest.writeString(url);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MinioMessage> CREATOR = new Creator<MinioMessage>() {
        @Override
        public MinioMessage createFromParcel(Parcel in) {
            return new MinioMessage(in);
        }

        @Override
        public MinioMessage[] newArray(int size) {
            return new MinioMessage[size];
        }
    };
}
