package io.nebula.example.mapper;

import io.nebula.data.persistence.mapper.BaseMapper;
import io.nebula.example.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查找用户
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    User findByEmail(@Param("email") String email);
    
    /**
     * 根据手机号查找用户
     */
    @Select("SELECT * FROM users WHERE phone = #{phone} AND deleted = 0")
    User findByPhone(@Param("phone") String phone);
    
    /**
     * 根据角色查找用户
     */
    @Select("SELECT * FROM users WHERE role = #{role} AND deleted = 0")
    List<User> findByRole(@Param("role") String role);
    
    /**
     * 根据状态查找用户
     */
    @Select("SELECT * FROM users WHERE status = #{status} AND deleted = 0")
    List<User> findByStatus(@Param("status") Integer status);
    
    /**
     * 查找最近登录的用户
     */
    @Select("SELECT * FROM users WHERE last_login_time >= #{sinceTime} AND deleted = 0 ORDER BY last_login_time DESC")
    List<User> findRecentlyLoggedIn(@Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * 分页查询用户
     */
    @Select("SELECT * FROM users WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<User> findUsersWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 统计用户总数
     */
    @Select("SELECT COUNT(*) FROM users WHERE deleted = 0")
    long countTotalUsers();
    
    /**
     * 根据角色统计用户数
     */
    @Select("SELECT COUNT(*) FROM users WHERE role = #{role} AND deleted = 0")
    long countUsersByRole(@Param("role") String role);
    
    /**
     * 根据状态统计用户数
     */
    @Select("SELECT COUNT(*) FROM users WHERE status = #{status} AND deleted = 0")
    long countUsersByStatus(@Param("status") Integer status);
}
