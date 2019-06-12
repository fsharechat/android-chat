package cn.wildfirechat.proto.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectMessage implements Parcelable {
    private String clientIdentifier;
    private String willTopic;
    private String willMessage;
    private String userName;
    private String password;

    public ConnectMessage(){

    }

    protected ConnectMessage(Parcel in) {
        clientIdentifier = in.readString();
        willTopic = in.readString();
        willMessage = in.readString();
        userName = in.readString();
        password = in.readString();
    }

    public static final Creator<ConnectMessage> CREATOR = new Creator<ConnectMessage>() {
        @Override
        public ConnectMessage createFromParcel(Parcel in) {
            return new ConnectMessage(in);
        }

        @Override
        public ConnectMessage[] newArray(int size) {
            return new ConnectMessage[size];
        }
    };

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public String getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(clientIdentifier);
        parcel.writeString(willTopic);
        parcel.writeString(willMessage);
        parcel.writeString(userName);
        parcel.writeString(password);
    }
}
