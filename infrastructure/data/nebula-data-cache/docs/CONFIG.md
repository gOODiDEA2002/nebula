# Nebula Data Cache 配置指南

> 多级缓存配置说明

## 概述

`nebula-data-cache` 提供多级缓存支持,组合本地缓存(Caffeine)和分布式缓存(Redis),提升系统性能。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-data-cache</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 本地缓存配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: local  # local / redis / multi-level
      local:
        # 最大缓存数量
        maximum-size: 10000
        # 过期时间(秒)
        expire-after-write: 600  # 10分钟
        expire-after-access: 300  # 5分钟无访问后过期
        # 初始容量
        initial-capacity: 100
        # 刷新时间
        refresh-after-write: 60  # 1分钟后刷新
```

### Redis缓存配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: redis
      redis:
        # Redis连接配置
        host: localhost
        port: 6379
        password: ${REDIS_PASSWORD}
        database: 0
        # 默认过期时间(秒)
        default-ttl: 3600  # 1小时
        # 键前缀
        key-prefix: "ticket:"
        # 序列化方式: jdk / json / protobuf
        serialization: json
        # 连接池配置
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000
```

### 多级缓存配置

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: multi-level
      multi-level:
        # L1: 本地缓存
        l1:
          enabled: true
          maximum-size: 1000
          expire-after-write: 300  # 5分钟
        # L2: Redis缓存
        l2:
          enabled: true
          host: localhost
          port: 6379
          default-ttl: 3600  # 1小时
```

## 票务系统场景

### 电影信息缓存

```yaml
nebula:
  data:
    cache:
      type: multi-level
      multi-level:
        l1:
          maximum-size: 500     # 缓存500部热门电影
          expire-after-write: 600  # 10分钟
        l2:
          default-ttl: 3600     # 1小时
      # 自定义缓存配置
      caches:
        movies:
          ttl: 1800  # 电影信息30分钟
        showtimes:
          ttl: 300   # 场次信息5分钟
        seats:
          ttl: 60    # 座位信息1分钟(实时性要求高)
```

### 使用示例

```java
@Service
public class MovieService {
    
    /**
     * 查询电影(自动缓存)
     */
    @Cacheable(value = "movies", key = "#id")
    public MovieVO getById(Long id) {
        Movie movie = movieMapper.selectById(id);
        return toVO(movie);
    }
    
    /**
     * 更新电影(清除缓存)
     */
    @CacheEvict(value = "movies", key = "#movie.id")
    public MovieVO update(Movie movie) {
        movieMapper.updateById(movie);
        return toVO(movie);
    }
    
    /**
     * 查询场次(短时间缓存)
     */
    @Cacheable(value = "showtimes", key = "#showtimeId")
    public ShowtimeVO getShowtime(Long showtimeId) {
        return showtimeMapper.selectById(showtimeId);
    }
}
```

### 座位缓存策略

```java
@Service
public class SeatService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 获取座位状态(Redis缓存,实时性高)
     */
    public SeatStatus getSeatStatus(Long showtimeId, String seatNo) {
        String cacheKey = "seat:" + showtimeId + ":" + seatNo;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return JsonUtils.fromJson(cached, SeatStatus.class);
        }
        
        // 从数据库查询
        SeatStatus status = querySeatStatus(showtimeId, seatNo);
        
        // 缓存1分钟
        redisTemplate.opsForValue().set(
            cacheKey,
            JsonUtils.toJson(status),
            1,
            TimeUnit.MINUTES
        );
        
        return status;
    }
}
```

## 缓存策略

### Cache Aside模式

```java
public MovieVO getMovie(Long id) {
    // 1. 查询缓存
    String cacheKey = "movie:" + id;
    MovieVO cached = (MovieVO) cacheManager.get(cacheKey);
    if (cached != null) {
        return cached;
    }
    
    // 2. 查询数据库
    Movie movie = movieMapper.selectById(id);
    MovieVO vo = toVO(movie);
    
    // 3. 写入缓存
    cacheManager.put(cacheKey, vo, Duration.ofMinutes(30));
    
    return vo;
}
```

### Read Through模式

```java
@Cacheable(value = "movies", key = "#id")
public MovieVO getMovie(Long id) {
    // Spring会自动处理缓存逻辑
    Movie movie = movieMapper.selectById(id);
    return toVO(movie);
}
```

### Write Through模式

```java
@CachePut(value = "movies", key = "#movie.id")
public MovieVO updateMovie(Movie movie) {
    movieMapper.updateById(movie);
    return toVO(movie);
}
```

## 性能优化

### 缓存预热

```java
@Component
public class CacheWarmer implements ApplicationRunner {
    
    @Autowired
    private MovieService movieService;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始缓存预热...");
        
        // 预热热门电影
        List<Movie> hotMovies = movieMapper.selectHotMovies(100);
        for (Movie movie : hotMovies) {
            movieService.getById(movie.getId());  // 触发缓存
        }
        
        log.info("缓存预热完成");
    }
}
```

### 缓存穿透防护

```java
@Service
public class MovieService {
    
    @Cacheable(value = "movies", key = "#id", unless = "#result == null")
    public MovieVO getById(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            // 缓存空值,防止穿透
            return MovieVO.empty();
        }
        return toVO(movie);
    }
}
```

### 缓存雪崩防护

```yaml
nebula:
  data:
    cache:
      multi-level:
        l2:
          # 随机过期时间,防止同时失效
          default-ttl: 3600
          ttl-random-range: 600  # 在3600±600秒之间随机
```

## 环境配置

### 开发环境

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: local  # 开发环境使用本地缓存
```

### 生产环境

```yaml
nebula:
  data:
    cache:
      enabled: true
      type: multi-level  # 生产环境使用多级缓存
      multi-level:
        l1:
          maximum-size: 10000
          expire-after-write: 300
        l2:
          host: ${REDIS_HOST}
          port: ${REDIS_PORT}
          password: ${REDIS_PASSWORD}
          default-ttl: 3600
          pool:
            max-active: 50
            max-idle: 20
```

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

