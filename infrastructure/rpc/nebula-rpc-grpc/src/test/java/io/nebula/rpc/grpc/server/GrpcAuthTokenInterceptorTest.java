package io.nebula.rpc.grpc.server;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GrpcAuthTokenInterceptor 测试
 */
@ExtendWith(MockitoExtension.class)
class GrpcAuthTokenInterceptorTest {

    @Mock
    private ServerCall<Object, Object> mockCall;

    @Mock
    private ServerCallHandler<Object, Object> mockNext;

    @Mock
    private ServerCall.Listener<Object> mockListener;

    @Test
    void noTokenConfigured_passThrough() {
        var interceptor = new GrpcAuthTokenInterceptor("");
        var headers = new Metadata();
        when(mockNext.startCall(any(), any())).thenReturn(mockListener);

        var result = interceptor.interceptCall(mockCall, headers, mockNext);

        assertThat(result).isSameAs(mockListener);
        verify(mockNext).startCall(mockCall, headers);
        verify(mockCall, never()).close(any(), any());
    }

    @Test
    void correctToken_passThrough() {
        var interceptor = new GrpcAuthTokenInterceptor("my-secret");
        var headers = new Metadata();
        headers.put(GrpcAuthTokenInterceptor.AUTH_TOKEN_KEY, "my-secret");
        when(mockNext.startCall(any(), any())).thenReturn(mockListener);

        var result = interceptor.interceptCall(mockCall, headers, mockNext);

        assertThat(result).isSameAs(mockListener);
        verify(mockNext).startCall(mockCall, headers);
    }

    @Test
    void wrongToken_unauthenticated() {
        var interceptor = new GrpcAuthTokenInterceptor("my-secret");
        var headers = new Metadata();
        headers.put(GrpcAuthTokenInterceptor.AUTH_TOKEN_KEY, "wrong-token");
        when(mockCall.getMethodDescriptor()).thenReturn(
                io.grpc.MethodDescriptor.<Object, Object>newBuilder()
                        .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName("test/method")
                        .setRequestMarshaller(mock(io.grpc.MethodDescriptor.Marshaller.class))
                        .setResponseMarshaller(mock(io.grpc.MethodDescriptor.Marshaller.class))
                        .build());

        interceptor.interceptCall(mockCall, headers, mockNext);

        var statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(mockCall).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(mockNext, never()).startCall(any(), any());
    }

    @Test
    void missingToken_unauthenticated() {
        var interceptor = new GrpcAuthTokenInterceptor("my-secret");
        var headers = new Metadata();
        when(mockCall.getMethodDescriptor()).thenReturn(
                io.grpc.MethodDescriptor.<Object, Object>newBuilder()
                        .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName("test/method")
                        .setRequestMarshaller(mock(io.grpc.MethodDescriptor.Marshaller.class))
                        .setResponseMarshaller(mock(io.grpc.MethodDescriptor.Marshaller.class))
                        .build());

        interceptor.interceptCall(mockCall, headers, mockNext);

        var statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(mockCall).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(mockNext, never()).startCall(any(), any());
    }
}
