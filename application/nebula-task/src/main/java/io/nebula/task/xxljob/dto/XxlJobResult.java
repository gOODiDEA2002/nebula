package io.nebula.task.xxljob.dto;

import io.nebula.task.core.TaskResult;

/**
 * XXL-JOB 执行结果
 */
public class XxlJobResult {
    
    private int code;
    private String msg;
    private Object content;
    
    public XxlJobResult() {}
    
    public XxlJobResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public XxlJobResult(TaskResult taskResult) {
        this.code = taskResult.isSuccess() ? 200 : 500;
        this.msg = taskResult.getMessage();
    }
    
    public static XxlJobResult success() {
        return new XxlJobResult(200, null);
    }
    
    public static XxlJobResult success(String msg) {
        return new XxlJobResult(200, msg);
    }
    
    public static XxlJobResult failure(String msg) {
        return new XxlJobResult(500, msg);
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
    
    public Object getContent() {
        return content;
    }
    
    public void setContent(Object content) {
        this.content = content;
    }
    
    public boolean isSuccess() {
        return code == 200;
    }
    
    @Override
    public String toString() {
        return String.format("XxlJobResult{code=%d, msg='%s', content=%s}", code, msg, content);
    }
}
