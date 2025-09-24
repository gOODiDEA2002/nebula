package io.nebula.core.common.constant;

/**
 * 系统常量定义
 * 定义系统中常用的常量值
 */
public final class Constants {
    
    /**
     * 私有构造函数，防止实例化
     */
    private Constants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ====================
    // 通用常量
    // ====================
    
    /**
     * 字符集
     */
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String CHARSET_GBK = "GBK";
    public static final String CHARSET_ISO_8859_1 = "ISO-8859-1";
    
    /**
     * 分隔符
     */
    public static final String SEPARATOR_COMMA = ",";
    public static final String SEPARATOR_SEMICOLON = ";";
    public static final String SEPARATOR_PIPE = "|";
    public static final String SEPARATOR_COLON = ":";
    public static final String SEPARATOR_HYPHEN = "-";
    public static final String SEPARATOR_UNDERSCORE = "_";
    public static final String SEPARATOR_DOT = ".";
    public static final String SEPARATOR_SLASH = "/";
    public static final String SEPARATOR_BACKSLASH = "\\";
    
    /**
     * 空字符串相关
     */
    public static final String EMPTY_STRING = "";
    public static final String SPACE = " ";
    public static final String NULL_STRING = "null";
    
    /**
     * 布尔值字符串
     */
    public static final String TRUE_STRING = "true";
    public static final String FALSE_STRING = "false";
    public static final String YES_STRING = "yes";
    public static final String NO_STRING = "no";
    public static final String ON_STRING = "on";
    public static final String OFF_STRING = "off";
    
    /**
     * 数字常量
     */
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int MINUS_ONE = -1;
    public static final long ZERO_LONG = 0L;
    public static final long ONE_LONG = 1L;
    public static final long MINUS_ONE_LONG = -1L;
    
    // ====================
    // HTTP相关常量
    // ====================
    
    /**
     * HTTP方法
     */
    public static final class HttpMethod {
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String PUT = "PUT";
        public static final String DELETE = "DELETE";
        public static final String PATCH = "PATCH";
        public static final String HEAD = "HEAD";
        public static final String OPTIONS = "OPTIONS";
        public static final String TRACE = "TRACE";
        
        private HttpMethod() {}
    }
    
    /**
     * HTTP状态码
     */
    public static final class HttpStatus {
        // 2xx Success
        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int ACCEPTED = 202;
        public static final int NO_CONTENT = 204;
        
        // 3xx Redirection
        public static final int MOVED_PERMANENTLY = 301;
        public static final int FOUND = 302;
        public static final int NOT_MODIFIED = 304;
        
        // 4xx Client Error
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int METHOD_NOT_ALLOWED = 405;
        public static final int CONFLICT = 409;
        public static final int UNPROCESSABLE_ENTITY = 422;
        public static final int TOO_MANY_REQUESTS = 429;
        
        // 5xx Server Error
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int NOT_IMPLEMENTED = 501;
        public static final int BAD_GATEWAY = 502;
        public static final int SERVICE_UNAVAILABLE = 503;
        public static final int GATEWAY_TIMEOUT = 504;
        
        private HttpStatus() {}
    }
    
    /**
     * HTTP头名称
     */
    public static final class HttpHeader {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String CONTENT_LENGTH = "Content-Length";
        public static final String CONTENT_ENCODING = "Content-Encoding";
        public static final String ACCEPT = "Accept";
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        public static final String ACCEPT_LANGUAGE = "Accept-Language";
        public static final String AUTHORIZATION = "Authorization";
        public static final String USER_AGENT = "User-Agent";
        public static final String REFERER = "Referer";
        public static final String COOKIE = "Cookie";
        public static final String SET_COOKIE = "Set-Cookie";
        public static final String CACHE_CONTROL = "Cache-Control";
        public static final String LOCATION = "Location";
        public static final String X_FORWARDED_FOR = "X-Forwarded-For";
        public static final String X_REAL_IP = "X-Real-IP";
        public static final String X_REQUEST_ID = "X-Request-ID";
        
        private HttpHeader() {}
    }
    
    /**
     * 媒体类型
     */
    public static final class MediaType {
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_XML = "application/xml";
        public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
        public static final String TEXT_XML = "text/xml";
        public static final String IMAGE_JPEG = "image/jpeg";
        public static final String IMAGE_PNG = "image/png";
        public static final String IMAGE_GIF = "image/gif";
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
        
        private MediaType() {}
    }
    
    // ====================
    // 错误码常量
    // ====================
    
    /**
     * 系统错误码
     */
    public static final class ErrorCode {
        // 成功
        public static final String SUCCESS = "0000";
        
        // 系统错误（1xxx）
        public static final String SYSTEM_ERROR = "1000";
        public static final String SYSTEM_BUSY = "1001";
        public static final String SYSTEM_TIMEOUT = "1002";
        public static final String SYSTEM_MAINTENANCE = "1003";
        
        // 参数错误（2xxx）
        public static final String PARAM_ERROR = "2000";
        public static final String PARAM_MISSING = "2001";
        public static final String PARAM_INVALID = "2002";
        public static final String PARAM_TYPE_ERROR = "2003";
        
        // 业务错误（3xxx）
        public static final String BUSINESS_ERROR = "3000";
        public static final String DATA_NOT_FOUND = "3001";
        public static final String DATA_ALREADY_EXISTS = "3002";
        public static final String DATA_CONFLICT = "3003";
        public static final String OPERATION_NOT_ALLOWED = "3004";
        public static final String RESOURCE_EXHAUSTED = "3005";
        
        // 认证授权错误（4xxx）
        public static final String AUTH_ERROR = "4000";
        public static final String AUTH_TOKEN_MISSING = "4001";
        public static final String AUTH_TOKEN_INVALID = "4002";
        public static final String AUTH_TOKEN_EXPIRED = "4003";
        public static final String AUTH_PERMISSION_DENIED = "4004";
        public static final String AUTH_USER_NOT_FOUND = "4005";
        public static final String AUTH_PASSWORD_ERROR = "4006";
        
        // 外部服务错误（5xxx）
        public static final String EXTERNAL_SERVICE_ERROR = "5000";
        public static final String EXTERNAL_SERVICE_TIMEOUT = "5001";
        public static final String EXTERNAL_SERVICE_UNAVAILABLE = "5002";
        
        private ErrorCode() {}
    }
    
    // ====================
    // 缓存相关常量
    // ====================
    
    /**
     * 缓存键前缀
     */
    public static final class CacheKey {
        public static final String USER_PREFIX = "user:";
        public static final String SESSION_PREFIX = "session:";
        public static final String CONFIG_PREFIX = "config:";
        public static final String LOCK_PREFIX = "lock:";
        public static final String COUNTER_PREFIX = "counter:";
        
        private CacheKey() {}
    }
    
    /**
     * 缓存过期时间（秒）
     */
    public static final class CacheExpire {
        public static final long ONE_MINUTE = 60L;
        public static final long FIVE_MINUTES = 300L;
        public static final long TEN_MINUTES = 600L;
        public static final long THIRTY_MINUTES = 1800L;
        public static final long ONE_HOUR = 3600L;
        public static final long SIX_HOURS = 21600L;
        public static final long TWELVE_HOURS = 43200L;
        public static final long ONE_DAY = 86400L;
        public static final long ONE_WEEK = 604800L;
        public static final long ONE_MONTH = 2592000L;
        
        private CacheExpire() {}
    }
    
    // ====================
    // 数据库相关常量
    // ====================
    
    /**
     * 数据库字段名
     */
    public static final class DbField {
        public static final String ID = "id";
        public static final String CREATE_TIME = "create_time";
        public static final String UPDATE_TIME = "update_time";
        public static final String CREATE_BY = "create_by";
        public static final String UPDATE_BY = "update_by";
        public static final String DELETED = "deleted";
        public static final String VERSION = "version";
        public static final String STATUS = "status";
        
        private DbField() {}
    }
    
    /**
     * 通用状态值
     */
    public static final class Status {
        public static final int ENABLED = 1;
        public static final int DISABLED = 0;
        public static final int ACTIVE = 1;
        public static final int INACTIVE = 0;
        public static final int DELETED = 1;
        public static final int NOT_DELETED = 0;
        
        private Status() {}
    }
    
    // ====================
    // 分页相关常量
    // ====================
    
    /**
     * 分页参数
     */
    public static final class Page {
        public static final int DEFAULT_PAGE = 1;
        public static final int DEFAULT_SIZE = 10;
        public static final int MAX_SIZE = 1000;
        public static final String PAGE_PARAM = "page";
        public static final String SIZE_PARAM = "size";
        public static final String SORT_PARAM = "sort";
        
        private Page() {}
    }
    
    // ====================
    // 文件相关常量
    // ====================
    
    /**
     * 文件扩展名
     */
    public static final class FileExtension {
        public static final String JPEG = ".jpeg";
        public static final String JPG = ".jpg";
        public static final String PNG = ".png";
        public static final String GIF = ".gif";
        public static final String PDF = ".pdf";
        public static final String TXT = ".txt";
        public static final String DOC = ".doc";
        public static final String DOCX = ".docx";
        public static final String XLS = ".xls";
        public static final String XLSX = ".xlsx";
        public static final String ZIP = ".zip";
        public static final String RAR = ".rar";
        
        private FileExtension() {}
    }
    
    /**
     * 文件大小限制（字节）
     */
    public static final class FileSize {
        public static final long ONE_KB = 1024L;
        public static final long ONE_MB = 1024L * 1024L;
        public static final long ONE_GB = 1024L * 1024L * 1024L;
        public static final long DEFAULT_MAX_SIZE = 10L * ONE_MB; // 10MB
        public static final long IMAGE_MAX_SIZE = 5L * ONE_MB; // 5MB
        public static final long DOCUMENT_MAX_SIZE = 20L * ONE_MB; // 20MB
        
        private FileSize() {}
    }
    
    // ====================
    // 时间相关常量
    // ====================
    
    /**
     * 时间格式
     */
    public static final class TimeFormat {
        public static final String DATE = "yyyy-MM-dd";
        public static final String DATETIME = "yyyy-MM-dd HH:mm:ss";
        public static final String TIME = "HH:mm:ss";
        public static final String DATETIME_COMPACT = "yyyyMMddHHmmss";
        public static final String DATE_COMPACT = "yyyyMMdd";
        public static final String ISO_DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        
        private TimeFormat() {}
    }
    
    /**
     * 时间间隔（毫秒）
     */
    public static final class TimeInterval {
        public static final long ONE_SECOND = 1000L;
        public static final long ONE_MINUTE = 60 * ONE_SECOND;
        public static final long ONE_HOUR = 60 * ONE_MINUTE;
        public static final long ONE_DAY = 24 * ONE_HOUR;
        public static final long ONE_WEEK = 7 * ONE_DAY;
        
        private TimeInterval() {}
    }
    
    // ====================
    // 正则表达式常量
    // ====================
    
    /**
     * 常用正则表达式
     */
    public static final class Regex {
        public static final String EMAIL = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        public static final String PHONE = "^1[3-9]\\d{9}$";
        public static final String ID_CARD = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";
        public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$";
        public static final String USERNAME = "^[a-zA-Z0-9_]{3,20}$";
        public static final String IP_ADDRESS = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        public static final String URL = "^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$";
        public static final String NUMERIC = "^\\d+$";
        public static final String DECIMAL = "^\\d+(\\.\\d+)?$";
        
        private Regex() {}
    }
    
    // ====================
    // 系统配置常量
    // ====================
    
    /**
     * 配置键名
     */
    public static final class ConfigKey {
        public static final String SYSTEM_NAME = "system.name";
        public static final String SYSTEM_VERSION = "system.version";
        public static final String SYSTEM_ENV = "system.env";
        public static final String LOG_LEVEL = "log.level";
        public static final String DATABASE_URL = "database.url";
        public static final String REDIS_HOST = "redis.host";
        public static final String JWT_SECRET = "jwt.secret";
        public static final String FILE_UPLOAD_PATH = "file.upload.path";
        
        private ConfigKey() {}
    }
    
    /**
     * 环境类型
     */
    public static final class Environment {
        public static final String DEVELOPMENT = "dev";
        public static final String TEST = "test";
        public static final String STAGING = "staging";
        public static final String PRODUCTION = "prod";
        
        private Environment() {}
    }
}
