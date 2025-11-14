package io.nebula.data.persistence.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.nebula.data.persistence.mapper.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * ServiceImpl单元测试
 * 测试Nebula扩展的Service层基础功能
 */
@ExtendWith(MockitoExtension.class)
class ServiceImplTest {
    
    @Mock
    private BaseMapper<TestEntity> baseMapper;
    
    @InjectMocks
    private TestServiceImpl service;
    
    @BeforeEach
    void setUp() {
        // 通过反射设置baseMapper
        ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
        
        // 使用lenient模式避免UnnecessaryStubbingException
        lenient().when(baseMapper.selectById(anyLong())).thenReturn(new TestEntity(1L, "test"));
        lenient().when(baseMapper.insert(any(TestEntity.class))).thenReturn(1);
    }
    
    @Test
    void testFindById() {
        Long id = 1L;
        TestEntity entity = new TestEntity(id, "test");
        when(baseMapper.selectByIdOpt(id)).thenReturn(Optional.of(entity));
        
        Optional<TestEntity> result = service.findById(id);
        
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        verify(baseMapper).selectByIdOpt(id);
    }
    
    @Test
    void testFindByIdNotFound() {
        Long id = 999L;
        when(baseMapper.selectByIdOpt(id)).thenReturn(Optional.empty());
        
        Optional<TestEntity> result = service.findById(id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void testFindOne() {
        TestEntity entity = new TestEntity(1L, "test");
        QueryWrapper<TestEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("name", "test");
        
        when(baseMapper.selectOneOpt(wrapper)).thenReturn(Optional.of(entity));
        
        Optional<TestEntity> result = service.findOne(wrapper);
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("test");
    }
    
    @Test
    void testSave() {
        TestEntity entity = new TestEntity(null, "new entity");
        when(baseMapper.insert(entity)).thenReturn(1);
        
        boolean result = service.save(entity);
        
        assertThat(result).isTrue();
        verify(baseMapper).insert(entity);
    }
    
    @Test
    void testUpdateById() {
        TestEntity entity = new TestEntity(1L, "updated");
        when(baseMapper.updateById(entity)).thenReturn(1);
        
        boolean result = service.updateById(entity);
        
        assertThat(result).isTrue();
        verify(baseMapper).updateById(entity);
    }
    
    @Test
    void testRemoveById() {
        Long id = 1L;
        when(baseMapper.deleteById(id)).thenReturn(1);
        
        boolean result = service.removeById(id);
        
        assertThat(result).isTrue();
        verify(baseMapper).deleteById(id);
    }
    
    @Test
    void testList() {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, "entity1"),
                new TestEntity(2L, "entity2")
        );
        when(baseMapper.selectList(any())).thenReturn(entities);
        
        List<TestEntity> result = service.list();
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("entity1");
    }
    
    @Test
    void testPage() {
        Page<TestEntity> page = new Page<>(1, 10);
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, "entity1"),
                new TestEntity(2L, "entity2")
        );
        Page<TestEntity> resultPage = new Page<TestEntity>(1, 10).setRecords(entities).setTotal(2);
        
        when(baseMapper.selectPage(any(Page.class), any())).thenReturn(resultPage);
        
        Page<TestEntity> result = service.page(page);
        
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2);
    }
    
    @Test
    void testCount() {
        when(baseMapper.selectCount(any())).thenReturn(5L);
        
        long count = service.count();
        
        assertThat(count).isEqualTo(5L);
    }
    
    // 测试实体类
    static class TestEntity {
        private Long id;
        private String name;
        
        public TestEntity() {}
        
        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    // 测试Service实现类
    static class TestServiceImpl extends ServiceImpl<BaseMapper<TestEntity>, TestEntity> {
        // 测试用的Service实现
    }
}

