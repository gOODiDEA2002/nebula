# nebula-foundation 模块单元测试清单

## 模块说明

Nebula 框架核心基础模块，提供常用工具类、统一异常处理、安全加密、结果封装等基础功能。

## 核心功能

1. 统一结果封装（Result类）
2. 异常处理体系（NebulaException、BusinessException、SystemException、ValidationException）
3. ID生成器（UUID、雪花算法、业务ID）
4. JWT工具（生成、解析、验证Token）
5. 加密工具（哈希、密码加密、AES加密、Base64编码）
6. JSON工具（序列化、反序列化）
7. 日期时间工具（格式化、解析、计算）

## 测试类清单

### 1. IdGeneratorTest

**测试类路径**: `io.nebula.foundation.id.IdGenerator`  
**测试目的**: 验证ID生成器各种ID生成功能的正确性

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testUuidGeneration() | uuid() | 测试UUID生成，验证格式为标准UUID格式（8-4-4-4-12） | 无 |
| testUuidSimple() | uuidSimple() | 测试无横线UUID生成，验证长度为32位 | 无 |
| testUuidUpper() | uuidUpper() | 测试大写UUID生成 | 无 |
| testSnowflakeId() | snowflakeId() | 测试雪花算法ID生成，验证唯一性和递增性 | 无 |
| testSnowflakeIdString() | snowflakeIdString() | 测试雪花算法ID字符串生成 | 无 |
| testCreateSnowflake() | createSnowflake(long, long) | 测试自定义雪花算法实例创建 | 无 |
| testOrderNo() | orderNo() | 测试订单号生成，验证格式为yyyyMMddHHmmss+随机数 | 无 |
| testUserId() | userId() | 测试用户ID生成，验证长度为8位 | 无 |
| testPrefixedId() | prefixedId(String, int) | 测试带前缀ID生成，验证前缀和长度 | 无 |
| testPrefixedNumericId() | prefixedNumericId(String, int) | 测试带前缀数字ID生成 | 无 |
| testShortId() | shortId() | 测试短ID生成，默认8位 | 无 |

**测试数据准备**: 无需特殊准备  
**验证要点**:
- UUID格式符合标准
- 雪花ID唯一且递增
- 业务ID格式符合预期
- 线程安全性（可选）

---

### 2. JwtUtilsTest

**测试类路径**: `io.nebula.foundation.security.JwtUtils`  
**测试目的**: 验证JWT工具类的Token生成、解析和验证功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGenerateKey() | generateKey() | 测试密钥生成，验证密钥不为空 | 无 |
| testKeyToBase64() | keyToBase64(SecretKey) | 测试密钥Base64编码 | 无 |
| testKeyFromBase64() | keyFromBase64(String) | 测试从Base64恢复密钥，验证与原密钥一致 | 无 |
| testGenerateToken() | generateToken(String, SecretKey) | 测试基本Token生成 | 无 |
| testGenerateTokenWithClaims() | generateToken(String, Map, SecretKey) | 测试带自定义声明的Token生成 | 无 |
| testGenerateTokenWithExpiration() | generateToken(String, Map, Duration, SecretKey) | 测试带过期时间的Token生成 | 无 |
| testParseToken() | parseToken(String, SecretKey) | 测试Token解析，验证主题和声明 | 无 |
| testIsTokenValid() | isTokenValid(String, SecretKey) | 测试Token有效性验证 | 无 |
| testGetSubject() | getSubject(String, SecretKey) | 测试获取Token主题 | 无 |
| testGetClaim() | getClaim(String, SecretKey, String, Class) | 测试获取特定声明 | 无 |
| testRefreshToken() | refreshToken(String, SecretKey, Duration) | 测试Token刷新 | 无 |

**测试数据准备**:
- 生成测试用密钥
- 准备测试用户ID和声明

**验证要点**:
- Token格式正确
- 声明数据完整
- 过期时间生效
- 密钥验证正确

---

### 3. CryptoUtilsTest

**测试类路径**: `io.nebula.foundation.security.CryptoUtils`  
**测试目的**: 验证加密工具类的各种加密和哈希功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testMd5() | md5(String) | 测试MD5哈希，验证结果长度为32位 | 无 |
| testSha1() | sha1(String) | 测试SHA-1哈希 | 无 |
| testSha256() | sha256(String) | 测试SHA-256哈希 | 无 |
| testSha256WithSalt() | sha256WithSalt(String, String) | 测试带盐的SHA-256哈希 | 无 |
| testEncrypt() | encrypt(String) | 测试密码加密，验证格式为salt:hash | 无 |
| testMatches() | matches(String, String) | 测试密码验证，验证正确密码匹配成功 | 无 |
| testMatchesWrongPassword() | matches(String, String) | 测试错误密码不匹配 | 无 |
| testIsStrongPassword() | isStrongPassword(String) | 测试密码强度检查 | 无 |
| testGenerateAESKey() | generateAESKey() | 测试AES密钥生成 | 无 |
| testAesEncrypt() | aesEncrypt(String, String) | 测试AES加密 | 无 |
| testAesDecrypt() | aesDecrypt(String, String) | 测试AES解密，验证解密后与原文一致 | 无 |
| testBase64Encode() | base64Encode(String) | 测试Base64编码 | 无 |
| testBase64Decode() | base64Decode(String) | 测试Base64解码，验证解码后与原文一致 | 无 |
| testBase64UrlEncode() | base64UrlEncode(String) | 测试URL安全的Base64编码 | 无 |
| testGenerateRandomString() | generateRandomString(int) | 测试随机字符串生成 | 无 |
| testGenerateSalt() | generateSalt(int) | 测试盐值生成 | 无 |
| testSecureEquals() | secureEquals(String, String) | 测试安全字符串比较 | 无 |

**测试数据准备**:
- 测试用明文字符串
- 测试用密码
- 测试用AES密钥

**验证要点**:
- 哈希结果一致性
- 加密解密正确性
- 密码验证准确性
- Base64编码解码正确

---

### 4. JsonUtilsTest

**测试类路径**: `io.nebula.foundation.json.JsonUtils`  
**测试目的**: 验证JSON工具类的序列化和反序列化功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testToJson() | toJson(Object) | 测试对象序列化为JSON字符串 | 无 |
| testToPrettyJson() | toPrettyJson(Object) | 测试美化输出JSON | 无 |
| testToJsonBytes() | toJsonBytes(Object) | 测试序列化为字节数组 | 无 |
| testFromJson() | fromJson(String, Class) | 测试JSON反序列化为对象 | 无 |
| testFromJsonWithTypeReference() | fromJson(String, TypeReference) | 测试使用TypeReference反序列化泛型 | 无 |
| testFromJsonBytes() | fromJsonBytes(byte[], Class) | 测试从字节数组反序列化 | 无 |
| testToMap() | toMap(String) | 测试JSON字符串转Map | 无 |
| testToList() | toList(String, Class) | 测试JSON字符串转List | 无 |
| testObjectToMap() | objectToMap(Object) | 测试对象转Map | 无 |
| testMapToObject() | mapToObject(Map, Class) | 测试Map转对象 | 无 |
| testIsValidJson() | isValidJson(String) | 测试JSON有效性验证 | 无 |
| testIsJsonObject() | isJsonObject(String) | 测试是否为JSON对象 | 无 |
| testIsJsonArray() | isJsonArray(String) | 测试是否为JSON数组 | 无 |
| testDeepCopy() | deepCopy(Object, Class) | 测试深拷贝 | 无 |
| testMergeJson() | mergeJson(String, String) | 测试合并两个JSON | 无 |

**测试数据准备**:
- 准备测试POJO类（User、Product等）
- 准备测试JSON字符串

**验证要点**:
- 序列化后JSON格式正确
- 反序列化后对象属性完整
- 泛型处理正确
- 特殊字符处理正确

---

### 5. DateUtilsTest

**测试类路径**: `io.nebula.foundation.time.DateUtils`  
**测试目的**: 验证日期时间工具类的格式化、解析和计算功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testNow() | now() | 测试当前日期获取 | 无 |
| testNowDateTime() | nowDateTime() | 测试当前日期时间获取 | 无 |
| testNowTimestamp() | nowTimestamp() | 测试当前时间戳获取（秒） | 无 |
| testNowTimestampMillis() | nowTimestampMillis() | 测试当前时间戳获取（毫秒） | 无 |
| testFormatDate() | formatDate(LocalDate) | 测试日期格式化，默认格式yyyy-MM-dd | 无 |
| testFormatDateWithPattern() | formatDate(LocalDate, String) | 测试自定义格式日期格式化 | 无 |
| testFormatDateTime() | formatDateTime(LocalDateTime) | 测试日期时间格式化 | 无 |
| testParseDate() | parseDate(String) | 测试日期解析 | 无 |
| testParseDateWithPattern() | parseDate(String, String) | 测试自定义格式日期解析 | 无 |
| testParseDateTime() | parseDateTime(String) | 测试日期时间解析 | 无 |
| testPlusDays() | plusDays(LocalDate, long) | 测试日期加天数 | 无 |
| testPlusMonths() | plusMonths(LocalDate, long) | 测试日期加月份 | 无 |
| testPlusYears() | plusYears(LocalDate, long) | 测试日期加年份 | 无 |
| testPlusHours() | plusHours(LocalDateTime, long) | 测试时间加小时 | 无 |
| testDaysBetween() | daysBetween(LocalDate, LocalDate) | 测试计算日期间隔天数 | 无 |
| testHoursBetween() | hoursBetween(LocalDateTime, LocalDateTime) | 测试计算时间间隔小时数 | 无 |
| testIsBetween() | isBetween(LocalDate, LocalDate, LocalDate) | 测试日期是否在范围内 | 无 |
| testStartOfMonth() | startOfMonth(LocalDate) | 测试获取月初日期 | 无 |
| testEndOfMonth() | endOfMonth(LocalDate) | 测试获取月末日期 | 无 |
| testStartOfYear() | startOfYear(LocalDate) | 测试获取年初日期 | 无 |
| testEndOfYear() | endOfYear(LocalDate) | 测试获取年末日期 | 无 |
| testToTimestamp() | toTimestamp(LocalDateTime, ZoneId) | 测试LocalDateTime转时间戳 | 无 |
| testFromTimestamp() | fromTimestamp(long, ZoneId) | 测试时间戳转LocalDateTime | 无 |

**测试数据准备**:
- 准备固定的测试日期（如2025-01-15）
- 准备测试日期字符串

**验证要点**:
- 格式化输出格式正确
- 解析结果准确
- 日期计算结果正确
- 时区转换准确

---

### 6. ResultTest

**测试类路径**: `io.nebula.foundation.result.Result`  
**测试目的**: 验证统一结果封装类的功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSuccess() | success(T) | 测试成功结果创建 | 无 |
| testSuccessWithMessage() | success(T, String) | 测试带消息的成功结果 | 无 |
| testError() | error(String, String) | 测试错误结果创建 | 无 |
| testBusinessError() | businessError(String) | 测试业务错误结果 | 无 |
| testValidationError() | validationError(String) | 测试验证错误结果 | 无 |
| testUnauthorized() | unauthorized(String) | 测试未授权结果 | 无 |
| testForbidden() | forbidden(String) | 测试禁止访问结果 | 无 |
| testWithRequestId() | withRequestId(String) | 测试添加请求ID | 无 |
| testIsSuccess() | isSuccess() | 测试判断是否成功 | 无 |

**测试数据准备**:
- 准备测试数据对象

**验证要点**:
- success标志正确
- code和message正确
- timestamp自动设置
- requestId正确传递

---

### 7. ExceptionTest

**测试类路径**: 异常类测试  
**测试目的**: 验证异常体系的正确性

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testBusinessExceptionOf() | BusinessException.of(String) | 测试业务异常创建 | 无 |
| testBusinessExceptionWithCode() | BusinessException.withCode(String, String, Object...) | 测试带错误码的业务异常 | 无 |
| testSystemException() | new SystemException(String, String) | 测试系统异常创建 | 无 |
| testValidationException() | new ValidationException(String, String) | 测试验证异常创建 | 无 |
| testGetFormattedMessage() | getFormattedMessage() | 测试格式化消息获取 | 无 |

**测试数据准备**:
- 准备异常消息和参数

**验证要点**:
- 异常继承关系正确
- 错误码和消息正确
- 格式化消息支持参数替换

---

## Mock策略

### 不需要Mock
此模块的所有类都是纯工具类，不依赖外部服务，**无需Mock**。

### 测试隔离
- 每个测试方法独立运行
- 测试数据不共享，避免相互影响

---

## 测试依赖

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

运行测试：
```bash
mvn test -pl nebula/core/nebula-foundation
```

查看测试报告：
```bash
mvn surefire-report:report
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- 无明显性能问题（单个测试方法 < 1秒）

