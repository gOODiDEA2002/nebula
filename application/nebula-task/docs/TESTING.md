# nebula-task 模块单元测试清单

## 模块说明

基于XXL-JOB的任务调度模块，提供统一的任务调度和执行框架，支持定时任务接口和任务监控。

## 核心功能

1. 任务执行器（TaskExecutor）
2. 定时任务接口（EveryMinuteExecute、EveryHourExecute等）
3. 任务上下文（TaskContext）
4. 任务结果（TaskResult）
5. 任务引擎（TaskEngine）

## 测试类清单

### 1. TaskExecutorTest

**测试类路径**: `io.nebula.task.TaskExecutor`接口实现  
**测试目的**: 验证任务执行器功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testExecuteSuccess() | execute(TaskContext) | 测试任务执行成功 | 无 |
| testExecuteFailure() | execute() | 测试任务执行失败 | 无 |
| testGetExecutorName() | getExecutorName() | 测试获取执行器名称 | 无 |
| testSupports() | supports(TaskType) | 测试任务类型支持 | 无 |

**测试数据准备**:
- 创建TaskExecutor实现类
- 准备TaskContext测试对象

**验证要点**:
- 任务正确执行
- 返回TaskResult正确
- 执行器名称正确
- 任务类型支持判断正确

**Mock示例**:
```java
@Component
class TestTaskExecutor implements TaskExecutor {
    
    @Override
    public String getExecutorName() {
        return "testTask";
    }
    
    @Override
    public TaskResult execute(TaskContext context) {
        String param = context.getStringParameter("param", "default");
        return TaskResult.success(context).withData("result", param);
    }
}

@Test
void testExecuteSuccess() {
    TestTaskExecutor executor = new TestTaskExecutor();
    
    TaskContext context = TaskContext.builder()
        .parameters(Map.of("param", "test-value"))
        .logId(1)
        .logDateTime(System.currentTimeMillis())
        .build();
    
    TaskResult result = executor.execute(context);
    
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getData()).containsEntry("result", "test-value");
}
```

---

### 2. TaskContextTest

**测试类路径**: `io.nebula.task.TaskContext`  
**测试目的**: 验证任务上下文功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGetStringParameter() | getStringParameter(String, String) | 测试获取字符串参数 | 无 |
| testGetIntParameter() | getIntParameter(String, int) | 测试获取整数参数 | 无 |
| testGetBooleanParameter() | getBooleanParameter(String, boolean) | 测试获取布尔参数 | 无 |
| testGetLogger() | getLogger() | 测试获取日志器 | 无 |
| testMissingParameter() | - | 测试缺少参数时使用默认值 | 无 |

**测试数据准备**:
- 创建包含各类参数的TaskContext

**验证要点**:
- 参数正确获取
- 默认值正确返回
- 类型转换正确
- 日志器可用

**Mock示例**:
```java
@Test
void testTaskContext() {
    Map<String, Object> params = Map.of(
        "stringParam", "test",
        "intParam", 100,
        "boolParam", true
    );
    
    TaskContext context = TaskContext.builder()
        .parameters(params)
        .logId(1)
        .logDateTime(System.currentTimeMillis())
        .build();
    
    assertThat(context.getStringParameter("stringParam", "default"))
        .isEqualTo("test");
    assertThat(context.getIntParameter("intParam", 0))
        .isEqualTo(100);
    assertThat(context.getBooleanParameter("boolParam", false))
        .isTrue();
    
    // 测试缺少的参数返回默认值
    assertThat(context.getStringParameter("missing", "default"))
        .isEqualTo("default");
}
```

---

### 3. ScheduledTaskInterfaceTest

**测试类路径**: 定时任务接口实现测试  
**测试目的**: 验证定时任务接口功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testEveryMinuteExecute() | execute() | 测试每分钟执行任务 | 无 |
| testEveryHourExecute() | execute() | 测试每小时执行任务 | 无 |
| testEveryDayExecute() | execute() | 测试每天执行任务 | 无 |

**测试数据准备**:
- 创建定时任务接口实现

**验证要点**:
- 任务正确执行
- 无异常抛出

**Mock示例**:
```java
@Component
class TestEveryHourTask implements EveryHourExecute {
    
    private final AtomicInteger executionCount = new AtomicInteger(0);
    
    @Override
    public void execute() {
        executionCount.incrementAndGet();
    }
    
    public int getExecutionCount() {
        return executionCount.get();
    }
}

@Test
void testEveryHourExecute() {
    TestEveryHourTask task = new TestEveryHourTask();
    
    task.execute();
    task.execute();
    
    assertThat(task.getExecutionCount()).isEqualTo(2);
}
```

---

### 4. TaskEngineTest

**测试类路径**: `io.nebula.task.TaskEngine`  
**测试目的**: 验证任务引擎功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testExecuteSync() | executeSync(String, Map, int, long) | 测试同步执行任务 | TaskExecutor |
| testExecuteSyncNotFound() | executeSync() | 测试执行不存在的任务 | - |
| testGetTaskLog() | getTaskLog(int) | 测试获取任务日志 | - |

**测试数据准备**:
- Mock TaskExecutor
- 注册任务到TaskEngine

**验证要点**:
- 任务正确执行
- 不存在的任务返回错误
- 任务日志正确记录

**Mock示例**:
```java
@Mock
private TaskExecutor mockExecutor;

@InjectMocks
private TaskEngine taskEngine;

@Test
void testExecuteSync() {
    String taskName = "testTask";
    Map<String, Object> params = Map.of("param", "value");
    int logId = 1;
    long logDateTime = System.currentTimeMillis();
    
    TaskContext expectedContext = TaskContext.builder()
        .parameters(params)
        .logId(logId)
        .logDateTime(logDateTime)
        .build();
    
    TaskResult expectedResult = TaskResult.success(expectedContext);
    
    when(mockExecutor.getExecutorName()).thenReturn(taskName);
    when(mockExecutor.execute(any(TaskContext.class)))
        .thenReturn(expectedResult);
    
    // 注册执行器
    taskEngine.registerExecutor(mockExecutor);
    
    TaskResult result = taskEngine.executeSync(taskName, params, logId, logDateTime);
    
    assertThat(result.isSuccess()).isTrue();
    verify(mockExecutor).execute(any(TaskContext.class));
}
```

---

### 5. TaskResultTest

**测试类路径**: `io.nebula.task.TaskResult`  
**测试目的**: 验证任务结果功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSuccessResult() | success(TaskContext) | 测试成功结果 | 无 |
| testFailureResult() | failure(TaskContext, Exception) | 测试失败结果 | 无 |
| testWithData() | withData(String, Object) | 测试添加数据 | 无 |
| testWithMessage() | withMessage(String) | 测试设置消息 | 无 |

**测试数据准备**:
- 准备TaskContext

**验证要点**:
- 成功结果正确创建
- 失败结果包含异常信息
- 数据正确设置
- 消息正确设置

**Mock示例**:
```java
@Test
void testTaskResult() {
    TaskContext context = TaskContext.builder()
        .logId(1)
        .logDateTime(System.currentTimeMillis())
        .build();
    
    // 测试成功结果
    TaskResult successResult = TaskResult.success(context)
        .withMessage("任务执行成功")
        .withData("count", 10);
    
    assertThat(successResult.isSuccess()).isTrue();
    assertThat(successResult.getMessage()).isEqualTo("任务执行成功");
    assertThat(successResult.getData()).containsEntry("count", 10);
    
    // 测试失败结果
    Exception exception = new RuntimeException("任务失败");
    TaskResult failureResult = TaskResult.failure(context, exception)
        .withMessage("执行出错");
    
    assertThat(failureResult.isSuccess()).isFalse();
    assertThat(failureResult.getMessage()).contains("执行出错");
    assertThat(failureResult.getException()).isEqualTo(exception);
}
```

---

### 6. XXLJobIntegrationTest

**测试类路径**: XXL-JOB集成测试  
**测试目的**: 验证与XXL-JOB的集成

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testExecutorRegistration() | - | 测试执行器注册 | XxlJobSpringExecutor |
| testJobHandlerMapping() | - | 测试JobHandler映射 | - |

**测试数据准备**:
- Mock XXL-JOB配置
- Mock XxlJobSpringExecutor

**验证要点**:
- 执行器正确注册
- JobHandler正确映射

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| TaskExecutor | 任务引擎测试 | Mock execute() |
| XxlJobSpringExecutor | XXL-JOB集成测试 | Mock start(), destroy() |

### 不需要Mock的功能
- TaskContext（使用真实对象）
- TaskResult（使用真实对象）
- 定时任务接口实现（使用真实对象）

---

## 测试依赖

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
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/application/nebula-task
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- TaskExecutor和TaskContext测试通过
- 定时任务接口测试通过
- TaskEngine和TaskResult测试通过

