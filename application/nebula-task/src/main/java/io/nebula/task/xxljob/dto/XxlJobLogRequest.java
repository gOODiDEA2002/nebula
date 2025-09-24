package io.nebula.task.xxljob.dto;

/**
 * XXL-JOB 日志请求
 */
public class XxlJobLogRequest {
    
    private long logDateTim;
    private int logId;
    private int fromLineNum;
    
    public long getLogDateTim() {
        return logDateTim;
    }
    
    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }
    
    public int getLogId() {
        return logId;
    }
    
    public void setLogId(int logId) {
        this.logId = logId;
    }
    
    public int getFromLineNum() {
        return fromLineNum;
    }
    
    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }
    
    @Override
    public String toString() {
        return String.format("XxlJobLogRequest{logId=%d, logDateTim=%d, fromLineNum=%d}", 
                logId, logDateTim, fromLineNum);
    }
}
