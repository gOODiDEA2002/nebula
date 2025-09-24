package io.nebula.core.common.enums;

/**
 * 响应结果编码枚举
 * 定义API响应的标准状态码
 */
public enum ResultCode implements EnumBase<String> {
    
    /**
     * 成功
     */
    SUCCESS("0000", "操作成功"),
    
    /**
     * 系统错误
     */
    SYSTEM_ERROR("1000", "系统内部错误"),
    SYSTEM_BUSY("1001", "系统繁忙，请稍后重试"),
    SYSTEM_TIMEOUT("1002", "系统响应超时"),
    SYSTEM_MAINTENANCE("1003", "系统维护中"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR("2000", "参数错误"),
    PARAM_MISSING("2001", "缺少必要参数"),
    PARAM_INVALID("2002", "参数格式无效"),
    PARAM_TYPE_ERROR("2003", "参数类型错误"),
    
    /**
     * 业务错误
     */
    BUSINESS_ERROR("3000", "业务处理失败"),
    DATA_NOT_FOUND("3001", "数据不存在"),
    DATA_ALREADY_EXISTS("3002", "数据已存在"),
    DATA_CONFLICT("3003", "数据冲突"),
    OPERATION_NOT_ALLOWED("3004", "操作不被允许"),
    RESOURCE_EXHAUSTED("3005", "资源已耗尽"),
    
    /**
     * 认证授权错误
     */
    AUTH_ERROR("4000", "认证失败"),
    AUTH_TOKEN_MISSING("4001", "认证令牌缺失"),
    AUTH_TOKEN_INVALID("4002", "认证令牌无效"),
    AUTH_TOKEN_EXPIRED("4003", "认证令牌已过期"),
    AUTH_PERMISSION_DENIED("4004", "权限不足"),
    AUTH_USER_NOT_FOUND("4005", "用户不存在"),
    AUTH_PASSWORD_ERROR("4006", "密码错误"),
    
    /**
     * 外部服务错误
     */
    EXTERNAL_SERVICE_ERROR("5000", "外部服务错误"),
    EXTERNAL_SERVICE_TIMEOUT("5001", "外部服务超时"),
    EXTERNAL_SERVICE_UNAVAILABLE("5002", "外部服务不可用");
    
    private final String code;
    private final String description;
    
    ResultCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @Override
    public String getCode() {
        return code;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为成功状态
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * 判断是否为错误状态
     * 
     * @return 是否错误
     */
    public boolean isError() {
        return this != SUCCESS;
    }
    
    /**
     * 根据编码获取结果码
     * 
     * @param code 编码
     * @return 结果码枚举
     */
    public static ResultCode getByCode(String code) {
        return EnumBase.EnumUtils.getByCode(ResultCode.class, code);
    }
    
    /**
     * 根据描述获取结果码
     * 
     * @param description 描述
     * @return 结果码枚举
     */
    public static ResultCode getByDescription(String description) {
        return EnumBase.EnumUtils.getByDescription(ResultCode.class, description);
    }
    
    /**
     * 检查编码是否存在
     * 
     * @param code 编码
     * @return 是否存在
     */
    public static boolean hasCode(String code) {
        return EnumBase.EnumUtils.hasCode(ResultCode.class, code);
    }
}
