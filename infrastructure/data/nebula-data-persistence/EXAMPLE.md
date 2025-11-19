# Nebula Data Persistence 使用示例

## 1. 快速开始 (Quick Start)

### 引入依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>${nebula.version}</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

## 2. 配置示例 (Configuration)

### 基础配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nebula_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

nebula:
  data:
    persistence:
      enabled: true
```

## 3. 代码示例 (Code Examples)

### 场景 1：定义 BaseEntity

框架提供了 `DefaultMetaObjectHandler` 自动填充以下字段，建议定义一个基类：

```java
@Data
public abstract class BaseEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
    
    @TableField(fill = FieldFill.INSERT)
    @TableLogic
    private Integer deleted; // 0:未删除, 1:已删除
}
```

### 场景 2：定义业务实体

```java
@Data
@TableName("t_user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    
    private String username;
    private String email;
}
```

### 场景 3：Mapper 接口

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 即可获得所有 CRUD 方法
}
```

### 场景 4：Service 实现

```java
@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    public void demo() {
        // 插入（自动填充 createTime, version=1, deleted=0）
        User user = new User();
        user.setUsername("test");
        save(user);
        
        // 查询
        User u = getById(user.getId());
        
        // 逻辑删除（实际执行 UPDATE t_user SET deleted=1 WHERE id=?）
        removeById(u.getId());
    }
}
```

## 4. 读写分离配置 (可选)

如果需要启用读写分离：

```yaml
nebula:
  data:
    persistence:
      read-write:
        enabled: true
        master:
          url: ...
        slaves:
          slave1:
            url: ...
```

使用 `@ReadDataSource` 或 `@WriteDataSource` 注解控制数据源。
