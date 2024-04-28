package cn.wildfire.chat.app;

import android.os.Environment;

/**
 * Created by heavyrain lee on 2017/11/24.
 */

public interface Config {

//    String IM_SERVER_HOST = "192.168.0.102";
    String IM_SERVER_HOST = "backend-tcp.fsharechat.cn";
//    String IM_SERVER_HOST = "172.16.46.201";
    int IM_SERVER_PORT = 6789;

//    String APP_SERVER_HOST = "192.168.0.102";
    String APP_SERVER_HOST = "backend-http.fsharechat.cn";
//    String APP_SERVER_HOST = "172.16.46.201";
    int APP_SERVER_PORT = 443;

    String ICE_ADDRESS = "turn:turn.fsharechat.cn:3478";
    String ICE_USERNAME = "comsince";
    String ICE_PASSWORD = "comsince";

    int DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND = 120;

    String SP_NAME = "config";
    String SP_KEY_SHOW_GROUP_MEMBER_ALIAS = "show_group_member_alias:%s";

    String VIDEO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/video";
    String AUDIO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/audio";
    String PHOTO_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/photo";
    String FILE_SAVE_DIR = Environment.getExternalStorageDirectory().getPath() + "/wfc/file";
}
