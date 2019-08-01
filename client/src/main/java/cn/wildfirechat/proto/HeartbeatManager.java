package cn.wildfirechat.proto;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

public class HeartbeatManager {
    private Log logger = LoggerFactory.getLogger(HeartbeatManager.class);

    private final long MIN_HEARTBEAT_INTERVAL = 2 * 60 * 1000;
    private final long MAX_HEARTBEAT_INTERVAL = 12 * 60 * 1000;
    private final long MIDDLE_HEARTBEAT_INTERVAL = 3 * 60 * 1000;
    private volatile long currentMaxHeartbeatInterval = MIN_HEARTBEAT_INTERVAL;
    private volatile long currentHeartbeatInterval = MIN_HEARTBEAT_INTERVAL;
    private volatile long currentScheduleTime = 0;
    private volatile long currentSendSuccessTime = 0;
    private volatile long currentExceptionTime = 0;
    private volatile boolean upSearchingMaxInterval = true;
    //连续心跳成功,中间任何一次间断进行重置
    private int currentHeartbeatSuccessNum = 0;

    public long currentHeartInterval(){
        return currentHeartbeatInterval;
    }

    /**
     * 这里不用考虑网络异常断开情况
     * 心跳间隔的选择只在计算中间间隔时间内是否能够继续发送数据，如果网络异常断开，心跳定时器会被清除，重新进行新的一轮计算
     * 计算的核心是步进计算与步减计算
     *
     * */
    public void reportHeartbeatExceptionTime(long exceptionTime){
        this.currentExceptionTime = exceptionTime;
        if(currentExceptionTime != 0 && currentScheduleTime != 0 && currentExceptionTime > currentScheduleTime){
            long interval = currentExceptionTime - currentScheduleTime;
            //比较当前时间间隔与最大心跳间隔的差值，即步进增加值
            long diff = interval - currentMaxHeartbeatInterval;
            logger.i("reportHeartbeatExceptionTime diff "+diff);
            long absDiff = Math.abs(diff);
            if(absDiff >= 0){
                //心跳失败，重置成功次数
                currentHeartbeatSuccessNum = 0;
                upSearchingMaxInterval = false;
                if(absDiff < 60 * 1000){
                    //等值试探失败
                    currentMaxHeartbeatInterval = interval - 30 * 1000;
                } else if (absDiff < 90 * 1000){
                    //步进试探失败
                    currentMaxHeartbeatInterval = interval - absDiff / 2;
                }
            }
        }
        if(currentMaxHeartbeatInterval < MIDDLE_HEARTBEAT_INTERVAL){
            upSearchingMaxInterval = true;
        }
        if(currentMaxHeartbeatInterval < MIN_HEARTBEAT_INTERVAL){
            currentMaxHeartbeatInterval = MIN_HEARTBEAT_INTERVAL;
        }
        if(currentMaxHeartbeatInterval > MAX_HEARTBEAT_INTERVAL){
            currentMaxHeartbeatInterval = MAX_HEARTBEAT_INTERVAL;
        }
        currentHeartbeatInterval = currentMaxHeartbeatInterval;
        logger.i("reportHeartbeatExceptionTime current heart interval "+currentHeartbeatInterval+" max interval "+currentMaxHeartbeatInterval +" upSearchingMaxInterval "+upSearchingMaxInterval);
    }

    /**
     * 上报当前定时心跳的时间
     * @param currentTime 当前发起定时的时间
     * */
    public void reportHeartbeatScheduleTime(long currentTime){
        this.currentScheduleTime = currentTime;
    }

    public void reportHeartbeatSendSuccessTime(long successTime){
        this.currentSendSuccessTime = successTime;
        recalculateMaxHeartbeatInterval();
    }

    public synchronized long nextHeartbeatInterval(){
        if(upSearchingMaxInterval){
            currentHeartbeatInterval += 60 * 1000;
        } else {
            currentHeartbeatInterval = currentMaxHeartbeatInterval;
        }
        if(currentHeartbeatInterval > MAX_HEARTBEAT_INTERVAL){
            currentHeartbeatInterval = MAX_HEARTBEAT_INTERVAL;
        }
        logger.i("nextHeartbeatInterval current heart interval "+currentHeartbeatInterval+" max interval "+currentMaxHeartbeatInterval+" upSearchingMaxInterval "+upSearchingMaxInterval);
        return currentHeartbeatInterval;
    }

    /**
     * 重新计算心跳间隔
     * */
    private void recalculateMaxHeartbeatInterval(){
        if(currentSendSuccessTime != 0 && currentScheduleTime != 0){
            long interval = currentSendSuccessTime - currentScheduleTime;
            long diff = interval - currentMaxHeartbeatInterval;
            logger.i("recalculateMaxHeartbeatInterval diff "+diff);
            if(diff > 0 && diff < 90 * 1000){
                // 只有在合理间隔的增加值才算是有效的心跳间隔，心跳步进策略最大每次增加60s
                if(upSearchingMaxInterval){
                    currentMaxHeartbeatInterval = interval;
                    if(interval > MAX_HEARTBEAT_INTERVAL){
                        currentMaxHeartbeatInterval = MAX_HEARTBEAT_INTERVAL;
                    }
                }
                //防止tryheartbeat导致的无效次数
                if(!upSearchingMaxInterval){
                    currentHeartbeatSuccessNum++;
                    if(currentHeartbeatSuccessNum > 3){
                        long diffMaxHeartbeatTime = MAX_HEARTBEAT_INTERVAL - currentMaxHeartbeatInterval;
                        if(diffMaxHeartbeatTime >= 6 * 60 * 1000){
                            currentMaxHeartbeatInterval = currentMaxHeartbeatInterval + 30 * 1000;
                            currentHeartbeatSuccessNum = 0;
                            logger.i("recalculateMaxHeartbeatInterval add max Heartbeat interval currentMaxHeartbeatInterval "+currentMaxHeartbeatInterval);
                        }
                    }
                }
            }

            logger.i("recalculateMaxHeartbeatInterval interval "+interval+" current heart interval "+currentHeartbeatInterval+" max interval "+currentMaxHeartbeatInterval);
        }
    }

}
