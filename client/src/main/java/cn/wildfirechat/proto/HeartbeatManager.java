package cn.wildfirechat.proto;

import com.comsince.github.logger.Log;
import com.comsince.github.logger.LoggerFactory;

public class HeartbeatManager {
    private Log logger = LoggerFactory.getLogger(HeartbeatManager.class);

    private final long minHeartbeatInterval = 120 * 1000;
    private volatile long maxHeartbeatInterval = 600 * 1000;
    private volatile long currentMaxHeartbeatInterval = 0;
    private volatile long currentHeartbeatInterval = minHeartbeatInterval;
    private volatile long currentScheduleTime = 0;
    private volatile long currentSendSuccessTime = 0;
    private volatile long currentExceptionTime = 0;
    private boolean searchingMaxInterval = true;

    public long currentHeartInterval(){
        return currentHeartbeatInterval;
    }

    public void reportException(){
        currentScheduleTime = 0;
        currentSendSuccessTime = 0;
        currentExceptionTime = 0;
        if(currentHeartbeatInterval / 2 > minHeartbeatInterval){
            currentHeartbeatInterval = currentMaxHeartbeatInterval / 2;
        }
        searchingMaxInterval = true;
    }

    public void reportHeartbeatExceptionTime(long exceptionTime){
        this.currentExceptionTime = exceptionTime;
        if(currentExceptionTime != 0 && currentScheduleTime != 0 && currentExceptionTime > currentScheduleTime){
            long interval = currentExceptionTime - currentScheduleTime;
            if(interval > currentMaxHeartbeatInterval){
                currentMaxHeartbeatInterval = interval - 60 * 1000;
                searchingMaxInterval = false;
            } else {
                currentMaxHeartbeatInterval = interval - 30 * 1000;
            }
        } else {
            currentMaxHeartbeatInterval -= 30 * 1000;
        }
        currentHeartbeatInterval = currentMaxHeartbeatInterval;
        logger.i("reportHeartbeatExceptionTime current heart interval "+currentHeartbeatInterval+" max interval "+currentMaxHeartbeatInterval);
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
        if(searchingMaxInterval){
            currentHeartbeatInterval += 60 * 1000;
        } else {
            currentHeartbeatInterval = currentMaxHeartbeatInterval;
        }
        if(currentHeartbeatInterval > maxHeartbeatInterval){
            currentHeartbeatInterval = maxHeartbeatInterval;
        }
        logger.i("nextHeartbeatInterval current heart interval "+currentHeartbeatInterval+" max interval "+currentMaxHeartbeatInterval);
        return currentHeartbeatInterval;
    }

    private void recalculateMaxHeartbeatInterval(){
        if(currentSendSuccessTime != 0 && currentScheduleTime != 0){
            long interval = currentSendSuccessTime - currentScheduleTime;
            if(searchingMaxInterval){
                if(interval > currentMaxHeartbeatInterval){
                    currentMaxHeartbeatInterval = interval;
                }
                if(interval > maxHeartbeatInterval){
                    currentMaxHeartbeatInterval = maxHeartbeatInterval;
                }
            }
            logger.i("real interval "+interval+" current heart interval "+currentHeartbeatInterval+" max interval "+currentMaxHeartbeatInterval);
        }
    }

}
