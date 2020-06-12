package cn.wildfirechat.proto.upload;

public interface UploadProgressHandler {
    /**
     * 用户自定义进度处理类必须实现的方法
     *
     * @param upload 上传进度
     */
    void progress(int upload);
}
