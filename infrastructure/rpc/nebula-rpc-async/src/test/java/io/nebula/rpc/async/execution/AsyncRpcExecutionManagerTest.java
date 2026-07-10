package io.nebula.rpc.async.execution;

import io.nebula.rpc.async.storage.AsyncExecutionStorage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncRpcExecutionManagerTest {

    @Test
    void executionStartsWhenSavedRecordIsNotImmediatelyVisible() throws Exception {
        AsyncExecutionStorage storage = mock(AsyncExecutionStorage.class);
        AtomicReference<Runnable> queuedTask = new AtomicReference<>();
        AtomicBoolean callableInvoked = new AtomicBoolean();
        when(storage.findById(any())).thenReturn(null);

        AsyncRpcExecutionManager manager = new AsyncRpcExecutionManager(
                storage, queuedTask::set, JsonMapper.builder().build());
        Method method = TestService.class.getMethod("execute", String.class);

        manager.submitAsync(TestService.class, method, new Object[]{"input"}, () -> {
            callableInvoked.set(true);
            return "result";
        });
        queuedTask.get().run();

        assertThat(callableInvoked).isTrue();
    }

    @Test
    void cancelledQueuedExecutionDoesNotInvokeCallable() throws Exception {
        AsyncExecutionStorage storage = mock(AsyncExecutionStorage.class);
        AtomicReference<AsyncRpcExecution> storedExecution = new AtomicReference<>();
        AtomicReference<Runnable> queuedTask = new AtomicReference<>();

        doAnswer(invocation -> {
            storedExecution.set(invocation.getArgument(0));
            return null;
        }).when(storage).save(any(AsyncRpcExecution.class));
        when(storage.findById(any())).thenAnswer(invocation -> storedExecution.get());
        doAnswer(invocation -> {
            storedExecution.get().setStatus(invocation.getArgument(1));
            return null;
        }).when(storage).updateStatus(any(), any(ExecutionStatus.class));

        AsyncRpcExecutionManager manager = new AsyncRpcExecutionManager(
                storage, queuedTask::set, JsonMapper.builder().build());
        AtomicBoolean callableInvoked = new AtomicBoolean();
        Callable<String> callable = () -> {
            callableInvoked.set(true);
            return "result";
        };
        Method method = TestService.class.getMethod("execute", String.class);

        AsyncRpcExecution execution = manager.submitAsync(
                TestService.class, method, new Object[]{"input"}, callable);

        assertThat(manager.cancel(execution.getExecutionId())).isTrue();
        assertThat(storedExecution.get().getStatus()).isEqualTo(ExecutionStatus.CANCELLED);

        queuedTask.get().run();

        assertThat(storedExecution.get().getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
        assertThat(callableInvoked).isFalse();
        verify(storage, org.mockito.Mockito.never())
                .updateStatus(execution.getExecutionId(), ExecutionStatus.RUNNING);
    }

    interface TestService {
        String execute(String input);
    }
}
