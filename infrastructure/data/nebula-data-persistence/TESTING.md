# nebula-data-persistence 模块单元测试文档

> **模块**: nebula-data-persistence  
> **版本**: 2.0.1-SNAPSHOT  
> **最后更新**: 2025-01-13

## 📋 测试概述

### 测试目标

数据持久层模块，提供统一的数据访问抽象，基于MyBatis-Plus构建，集成读写分离、分库分表等功能的全面测试。

### 核心功能

1. 基础CRUD操作（BaseMapper、IService）
2. 分页查询
3. 批量操作
4. 读写分离（@ReadDataSource、@WriteDataSource）
5. 分库分表（ShardingSphere集成）

### 测试覆盖率目标

- **行覆盖率**: ≥ 90%
- **分支覆盖率**: ≥ 88%
- **核心业务逻辑**: 100%

## 🧪 测试用例设计

### 1. ServiceImplTest

**测试类路径**: `com.baomidou.mybatisplus.extension.service.impl.ServiceImpl`的子类  
**测试目的**: 验证MyBatis-Plus的Service层基础CRUD功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSave() | save(T) | 测试保存实体 | BaseMapper |
| testSaveBatch() | saveBatch(Collection) | 测试批量保存 | BaseMapper |
| testGetById() | getById(Serializable) | 测试根据ID查询 | BaseMapper |
| testUpdateById() | updateById(T) | 测试根据ID更新 | BaseMapper |
| testRemoveById() | removeById(Serializable) | 测试根据ID删除（逻辑删除） | BaseMapper |
| testPage() | page(Page, Wrapper) | 测试分页查询 | BaseMapper |
| testList() | list(Wrapper) | 测试列表查询 | BaseMapper |
| testCount() | count(Wrapper) | 测试统计数量 | BaseMapper |

**测试数据准备**:
- Mock BaseMapper（如UserMapper）
- 准备测试实体对象
- 准备测试查询条件

**验收标准**:
- ✅ CRUD操作正确
- ✅ 分页参数正确
- ✅ 查询条件正确
- ✅ 逻辑删除生效

**Mock示例**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("Should save user successfully")
    void testSave() {
        // Given
        User user = User.builder()
            .username("test")
            .name("Test User")
            .build();
        
        when(userMapper.insert(any(User.class))).thenReturn(1);
        
        // When
        boolean result = userService.save(user);
        
        // Then
        assertThat(result).isTrue();
        verify(userMapper).insert(user);
    }
}
```

### 2. ReadWriteSeparationTest

**测试类路径**: 读写分离功能测试  
**测试目的**: 验证@ReadDataSource和@WriteDataSource注解的数据源路由功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testReadDataSource() | @ReadDataSource注解的方法 | 测试读操作路由到从库 | DataSourceContextHolder |
| testWriteDataSource() | @WriteDataSource注解的方法 | 测试写操作路由到主库 | DataSourceContextHolder |
| testTransactionForceWrite() | @Transactional方法 | 测试事务中强制使用主库 | - |

**测试数据准备**:
- 配置多数据源
- 创建带注解的Service方法

**验收标准**:
- ✅ @ReadDataSource切换到从库
- ✅ @WriteDataSource切换到主库
- ✅ 事务中使用主库
- ✅ DataSourceContext正确设置

**Mock示例**:
```java
@Test
@DisplayName("Should route to read datasource when using @ReadDataSource")
void testReadDataSource() {
    // Given
    // 创建带@ReadDataSource注解的方法
    
    // When
    User user = productService.getProductById(1L);
    
    // Then
    // 验证使用了从库数据源
    String dataSource = DataSourceContextHolder.getDataSourceType();
    assertThat(dataSource).isEqualTo(DataSourceType.READ.name());
}
```

### 3. MapperTest

**测试类路径**: BaseMapper实现类  
**测试目的**: 验证Mapper接口的基本SQL操作

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testInsert() | insert(T) | 测试插入操作 | SqlSession |
| testSelectById() | selectById(Serializable) | 测试根据ID查询 | SqlSession |
| testUpdateById() | updateById(T) | 测试根据ID更新 | SqlSession |
| testDeleteById() | deleteById(Serializable) | 测试根据ID删除 | SqlSession |
| testSelectList() | selectList(Wrapper) | 测试列表查询 | SqlSession |

**测试数据准备**:
- Mock SqlSession
- 准备测试SQL和结果

**验收标准**:
- ✅ SQL正确执行
- ✅ 参数正确绑定
- ✅ 结果正确映射

## 🔧 Mock 策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| BaseMapper | Service层测试 | Mock CRUD方法 |
| SqlSession | Mapper层测试 | Mock selectOne(), selectList() |
| DataSource | 数据源测试 | Mock getConnection() |
| DataSourceContextHolder | 读写分离测试 | Mock setDataSourceType() |

### 不需要真实数据库

**所有测试都应该Mock数据库操作，不需要启动真实的数据库**。

## 📊 测试依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 🚀 测试执行

### 执行所有测试

```bash
# Maven
mvn test -pl infrastructure/data/nebula-data-persistence

# 执行特定测试类
mvn test -Dtest=UserServiceImplTest

# 生成覆盖率报告
mvn clean test jacoco:report
```

### 查看测试报告

```bash
# Surefire报告
mvn surefire-report:report

# 覆盖率报告
open target/site/jacoco/index.html
```

## 📝 测试验收标准

- ✅ 所有测试方法通过
- ✅ 核心功能测试覆盖率 >= 90%
- ✅ Mock对象使用正确，无真实数据库依赖
- ✅ 读写分离测试通过

## 📚 相关文档

- [模块 README](./README.md)
- [Nebula 框架使用指南](../../../docs/Nebula框架使用指南.md)
- [测试最佳实践](../../../docs/testing/BEST_PRACTICES.md)

---

**测试文档已迁移自** `/docs/test/nebula-data-persistence-test.md`

