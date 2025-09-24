package io.nebula.task.xxljob.dto;

/**
 * XXL-JOB 日志响应
 */
public class XxlJobLogResult {
    
    private int code;
    private String msg;
    private LogContent content;
    
    public XxlJobLogResult() {}
    
    public XxlJobLogResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public XxlJobLogResult(String logContent, int totalLines) {
        this.code = 200;
        this.msg = null;
        this.content = new LogContent(logContent, totalLines);
    }
    
    public static XxlJobLogResult success(String logContent, int totalLines) {
        return new XxlJobLogResult(logContent, totalLines);
    }
    
    public static XxlJobLogResult failure(String msg) {
        return new XxlJobLogResult(500, msg);
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMsg() {
        return msg;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public LogContent getContent() {
        return content;
    }
    
    public void setContent(LogContent content) {
        this.content = content;
    }
    
    /**
     * 日志内容
     */
    public static class LogContent {
        private int fromLineNum;
        private int toLineNum;
        private String logContent;
        private boolean isEnd;
        
        public LogContent() {}
        
        public LogContent(String logContent, int totalLines) {
            this.fromLineNum = 1;
            this.toLineNum = totalLines;
            this.logContent = logContent;
            this.isEnd = true;
        }
        
        public int getFromLineNum() {
            return fromLineNum;
        }
        
        public void setFromLineNum(int fromLineNum) {
            this.fromLineNum = fromLineNum;
        }
        
        public int getToLineNum() {
            return toLineNum;
        }
        
        public void setToLineNum(int toLineNum) {
            this.toLineNum = toLineNum;
        }
        
        public String getLogContent() {
            return logContent;
        }
        
        public void setLogContent(String logContent) {
            this.logContent = logContent;
        }
        
        public boolean isEnd() {
            return isEnd;
        }
        
        public void setEnd(boolean end) {
            isEnd = end;
        }
    }
}
