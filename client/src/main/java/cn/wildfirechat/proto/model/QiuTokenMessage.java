package cn.wildfirechat.proto.model;

import android.os.Parcel;
import android.os.Parcelable;

public class QiuTokenMessage implements Parcelable {
    private String token;
    private String domain;

    public QiuTokenMessage(){

    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    protected QiuTokenMessage(Parcel in) {
        token = in.readString();
        domain = in.readString();
    }

    public static final Creator<QiuTokenMessage> CREATOR = new Creator<QiuTokenMessage>() {
        @Override
        public QiuTokenMessage createFromParcel(Parcel in) {
            return new QiuTokenMessage(in);
        }

        @Override
        public QiuTokenMessage[] newArray(int size) {
            return new QiuTokenMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(token);
        parcel.writeString(domain);
    }
}
