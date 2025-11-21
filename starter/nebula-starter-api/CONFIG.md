# Nebula Starter API - 配置参考

> API契约模块专用Starter的配置说明，主要涉及Maven配置和依赖管理。

## 配置概览

- [基础配置](#基础配置)
- [依赖管理](#依赖管理)
- [版本管理](#版本管理)
- [票务系统配置示例](#票务系统配置示例)

---

## 基础配置

### Maven依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-api</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 包含的依赖

nebula-starter-api自动包含：

1. **nebula-rpc-core**：RPC核心注解
2. **spring-web**（provided）：Spring MVC注解
3. **jakarta.validation-api**：验证注解
4. **lombok**（provided）：减少样板代码

---

## 依赖管理

### Parent POM配置

如果有多个API契约模块，推荐创建Parent POM：

`api-parent/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>api-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <nebula.version>2.0.1-SNAPSHOT</nebula.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Nebula Starter API -->
            <dependency>
                <groupId>io.nebula</groupId>
                <artifactId>nebula-starter-api</artifactId>
                <version>${nebula.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>user-api</module>
        <module>order-api</module>
        <module>movie-api</module>
    </modules>
</project>
```

### 子模块配置

`user-api/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>api-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>user-api</artifactId>
    <name>User Service API</name>

    <dependencies>
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 版本管理

### 语义化版本

建议使用语义化版本（Semantic Versioning）：

- **主版本号**：不兼容的API修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

示例：

```xml
<version>1.0.0-SNAPSHOT</version>  <!-- 开发版本 -->
<version>1.0.0</version>           <!-- 正式版本 -->
<version>1.1.0</version>           <!-- 新增功能 -->
<version>2.0.0</version>           <!-- 不兼容变更 -->
```

### Maven版本插件

```xml
<build>
    <plugins>
        <!-- Maven Version Plugin -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>versions-maven-plugin</artifactId>
            <version>2.16.2</version>
        </plugin>
    </plugins>
</build>
```

使用命令：

```bash
# 更新版本
mvn versions:set -DnewVersion=1.1.0

# 提交版本更改
mvn versions:commit

# 回滚版本更改
mvn versions:revert
```

---

## 票务系统配置示例

### 项目结构

```
ticket-api/
├── pom.xml
├── user-api/
│   └── pom.xml
├── movie-api/
│   └── pom.xml
├── order-api/
│   └── pom.xml
└── common-api/
    └── pom.xml
```

### 父POM配置

`ticket-api/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ticketsystem</groupId>
    <artifactId>ticket-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Ticket System API Parent</name>
    <description>Ticket System API Contracts</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <nebula.version>2.0.1-SNAPSHOT</nebula.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Nebula Starter API -->
            <dependency>
                <groupId>io.nebula</groupId>
                <artifactId>nebula-starter-api</artifactId>
                <version>${nebula.version}</version>
            </dependency>
            
            <!-- Common API -->
            <dependency>
                <groupId>com.ticketsystem</groupId>
                <artifactId>common-api</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>common-api</module>
        <module>user-api</module>
        <module>movie-api</module>
        <module>order-api</module>
    </modules>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Common API模块

`common-api/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ticketsystem</groupId>
        <artifactId>ticket-api</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>common-api</artifactId>
    <name>Common API</name>
    <description>Common DTOs and Entities</description>

    <dependencies>
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

### User API模块

`user-api/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ticketsystem</groupId>
        <artifactId>ticket-api</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>user-api</artifactId>
    <name>User API</name>
    <description>User Service API Contract</description>

    <dependencies>
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-api</artifactId>
        </dependency>
        
        <!-- 依赖Common API -->
        <dependency>
            <groupId>com.ticketsystem</groupId>
            <artifactId>common-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 部署配置

### Maven仓库配置

`settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>${env.NEXUS_USERNAME}</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>${env.NEXUS_USERNAME}</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

`pom.xml`:

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>http://nexus.example.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>http://nexus.example.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

### 部署命令

```bash
# 部署到Maven仓库
mvn clean deploy

# 跳过测试部署
mvn clean deploy -DskipTests

# 发布正式版本
mvn versions:set -DnewVersion=1.0.0
mvn clean deploy
```

---

## 最佳实践

### 实践1：独立版本管理

每个API模块独立管理版本：

```xml
<groupId>com.example</groupId>
<artifactId>user-api</artifactId>
<version>1.2.0</version>
```

### 实践2：使用Properties管理版本

```xml
<properties>
    <user-api.version>1.2.0</user-api.version>
    <order-api.version>1.1.0</order-api.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>user-api</artifactId>
        <version>${user-api.version}</version>
    </dependency>
</dependencies>
```

### 实践3：BOM（Bill of Materials）

创建BOM统一管理API版本：

`api-bom/pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>user-api</artifactId>
            <version>1.2.0</version>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>order-api</artifactId>
            <version>1.1.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

使用BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>api-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

