package io.nebula.security.authentication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * SecurityContext单元测试
 */
class SecurityContextTest {
    
    @AfterEach
    void tearDown() {
        // 每个测试后清理SecurityContext
        SecurityContext.clearAuthentication();
    }
    
    @Test
    void testSetAuthentication() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser");
        JwtAuthenticationToken authentication = new JwtAuthenticationToken("token", principal, Arrays.asList());
        
        SecurityContext.setAuthentication(authentication);
        
        Authentication result = SecurityContext.getAuthentication();
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(authentication);
    }
    
    @Test
    void testGetAuthentication() {
        assertThat(SecurityContext.getAuthentication()).isNull();
        
        UserPrincipal principal = new UserPrincipal(2L, "user2");
        JwtAuthenticationToken authentication = new JwtAuthenticationToken("token", principal, Arrays.asList());
        SecurityContext.setAuthentication(authentication);
        
        Authentication result = SecurityContext.getAuthentication();
        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isEqualTo(principal);
    }
    
    @Test
    void testGetCurrentUserId() {
        assertThat(SecurityContext.getCurrentUserId()).isNull();
        
        UserPrincipal principal = new UserPrincipal(999L, "testuser");
        JwtAuthenticationToken authentication = new JwtAuthenticationToken("token", principal, Arrays.asList());
        SecurityContext.setAuthentication(authentication);
        
        Long userId = SecurityContext.getCurrentUserId();
        assertThat(userId).isEqualTo(999L);
    }
    
    @Test
    void testGetCurrentUsername() {
        assertThat(SecurityContext.getCurrentUsername()).isNull();
        
        UserPrincipal principal = new UserPrincipal(1L, "myusername");
        JwtAuthenticationToken authentication = new JwtAuthenticationToken("token", principal, Arrays.asList());
        SecurityContext.setAuthentication(authentication);
        
        String username = SecurityContext.getCurrentUsername();
        assertThat(username).isEqualTo("myusername");
    }
    
    @Test
    void testClearAuthentication() {
        UserPrincipal principal = new UserPrincipal(1L, "user");
        JwtAuthenticationToken authentication = new JwtAuthenticationToken("token", principal, Arrays.asList());
        SecurityContext.setAuthentication(authentication);
        
        assertThat(SecurityContext.getAuthentication()).isNotNull();
        
        SecurityContext.clearAuthentication();
        
        assertThat(SecurityContext.getAuthentication()).isNull();
        assertThat(SecurityContext.getCurrentUserId()).isNull();
        assertThat(SecurityContext.getCurrentUsername()).isNull();
    }
    
    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        // 主线程设置认证信息
        UserPrincipal mainPrincipal = new UserPrincipal(1L, "mainuser");
        JwtAuthenticationToken mainAuth = new JwtAuthenticationToken("main-token", mainPrincipal, Arrays.asList());
        SecurityContext.setAuthentication(mainAuth);
        
        assertThat(SecurityContext.getCurrentUserId()).isEqualTo(1L);
        assertThat(SecurityContext.getCurrentUsername()).isEqualTo("mainuser");
        
        // 在新线程中设置不同的认证信息
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Long> otherThreadUserId = new AtomicReference<>();
        AtomicReference<String> otherThreadUsername = new AtomicReference<>();
        
        Thread otherThread = new Thread(() -> {
            UserPrincipal otherPrincipal = new UserPrincipal(2L, "otheruser");
            JwtAuthenticationToken otherAuth = new JwtAuthenticationToken("other-token", otherPrincipal, Arrays.asList());
            SecurityContext.setAuthentication(otherAuth);
            
            otherThreadUserId.set(SecurityContext.getCurrentUserId());
            otherThreadUsername.set(SecurityContext.getCurrentUsername());
            
            latch.countDown();
        });
        
        otherThread.start();
        latch.await();
        otherThread.join();
        
        // 验证其他线程的认证信息
        assertThat(otherThreadUserId.get()).isEqualTo(2L);
        assertThat(otherThreadUsername.get()).isEqualTo("otheruser");
        
        // 验证主线程的认证信息没有被影响
        assertThat(SecurityContext.getCurrentUserId()).isEqualTo(1L);
        assertThat(SecurityContext.getCurrentUsername()).isEqualTo("mainuser");
    }
    
    @Test
    void testGetCurrentUserIdWhenPrincipalIsNotUserPrincipal() {
        // 设置一个非UserPrincipal的principal
        Authentication authentication = new Authentication() {
            @Override
            public Object getPrincipal() {
                return "string-principal";
            }
            
            @Override
            public Object getCredentials() {
                return null;
            }
            
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return null;
            }
            
            @Override
            public Object getDetails() {
                return null;
            }
            
            @Override
            public boolean isAuthenticated() {
                return true;
            }
            
            @Override
            public void setAuthenticated(boolean authenticated) {
            }
            
            @Override
            public java.util.Map<String, Object> getAttributes() {
                return null;
            }
            
            @Override
            public void setAttribute(String key, Object value) {
            }
            
            @Override
            public Object getAttribute(String key) {
                return null;
            }
        };
        
        SecurityContext.setAuthentication(authentication);
        
        assertThat(SecurityContext.getCurrentUserId()).isNull();
        assertThat(SecurityContext.getCurrentUsername()).isNull();
    }
}

