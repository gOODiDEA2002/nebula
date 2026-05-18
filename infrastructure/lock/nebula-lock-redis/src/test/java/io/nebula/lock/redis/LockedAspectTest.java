package io.nebula.lock.redis;

import io.nebula.lock.Lock;
import io.nebula.lock.LockConfig;
import io.nebula.lock.LockManager;
import io.nebula.lock.Locked;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LockedAspectTest {

    @Test
    void lockedAspectRunsBeforeTransactionalAdvice() {
        Order order = AnnotationUtils.findAnnotation(LockedAspect.class, Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void lockedAspectBindsAnnotationWhenInvokedBySpringAop() throws Throwable {
        LockManager lockManager = mock(LockManager.class);
        Lock lock = mock(Lock.class);
        when(lockManager.getLock(eq("test:service"), any(LockConfig.class))).thenReturn(lock);
        when(lock.tryLock(eq(1L), eq(TimeUnit.SECONDS))).thenReturn(true);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new LockedService());
        proxyFactory.addAspect(new LockedAspect(lockManager));
        LockedService proxy = proxyFactory.getProxy();

        assertThat(proxy.execute()).isEqualTo("ok");
        verify(lock).unlock();
    }

    static class LockedService {

        @Locked(key = "'test:service'", waitTime = 1, leaseTime = 5)
        String execute() {
            return "ok";
        }
    }
}
