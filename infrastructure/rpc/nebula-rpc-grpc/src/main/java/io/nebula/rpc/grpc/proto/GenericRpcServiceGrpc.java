package io.nebula.rpc.grpc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * 通用RPC服务定义
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: rpc_common.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class GenericRpcServiceGrpc {

  private GenericRpcServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "io.nebula.rpc.grpc.GenericRpcService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getCallMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Call",
      requestType = io.nebula.rpc.grpc.proto.RpcRequest.class,
      responseType = io.nebula.rpc.grpc.proto.RpcResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getCallMethod() {
    io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse> getCallMethod;
    if ((getCallMethod = GenericRpcServiceGrpc.getCallMethod) == null) {
      synchronized (GenericRpcServiceGrpc.class) {
        if ((getCallMethod = GenericRpcServiceGrpc.getCallMethod) == null) {
          GenericRpcServiceGrpc.getCallMethod = getCallMethod =
              io.grpc.MethodDescriptor.<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Call"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GenericRpcServiceMethodDescriptorSupplier("Call"))
              .build();
        }
      }
    }
    return getCallMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getServerStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ServerStream",
      requestType = io.nebula.rpc.grpc.proto.RpcRequest.class,
      responseType = io.nebula.rpc.grpc.proto.RpcResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getServerStreamMethod() {
    io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse> getServerStreamMethod;
    if ((getServerStreamMethod = GenericRpcServiceGrpc.getServerStreamMethod) == null) {
      synchronized (GenericRpcServiceGrpc.class) {
        if ((getServerStreamMethod = GenericRpcServiceGrpc.getServerStreamMethod) == null) {
          GenericRpcServiceGrpc.getServerStreamMethod = getServerStreamMethod =
              io.grpc.MethodDescriptor.<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ServerStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GenericRpcServiceMethodDescriptorSupplier("ServerStream"))
              .build();
        }
      }
    }
    return getServerStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getClientStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ClientStream",
      requestType = io.nebula.rpc.grpc.proto.RpcRequest.class,
      responseType = io.nebula.rpc.grpc.proto.RpcResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getClientStreamMethod() {
    io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse> getClientStreamMethod;
    if ((getClientStreamMethod = GenericRpcServiceGrpc.getClientStreamMethod) == null) {
      synchronized (GenericRpcServiceGrpc.class) {
        if ((getClientStreamMethod = GenericRpcServiceGrpc.getClientStreamMethod) == null) {
          GenericRpcServiceGrpc.getClientStreamMethod = getClientStreamMethod =
              io.grpc.MethodDescriptor.<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ClientStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GenericRpcServiceMethodDescriptorSupplier("ClientStream"))
              .build();
        }
      }
    }
    return getClientStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getBidirectionalStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BidirectionalStream",
      requestType = io.nebula.rpc.grpc.proto.RpcRequest.class,
      responseType = io.nebula.rpc.grpc.proto.RpcResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest,
      io.nebula.rpc.grpc.proto.RpcResponse> getBidirectionalStreamMethod() {
    io.grpc.MethodDescriptor<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse> getBidirectionalStreamMethod;
    if ((getBidirectionalStreamMethod = GenericRpcServiceGrpc.getBidirectionalStreamMethod) == null) {
      synchronized (GenericRpcServiceGrpc.class) {
        if ((getBidirectionalStreamMethod = GenericRpcServiceGrpc.getBidirectionalStreamMethod) == null) {
          GenericRpcServiceGrpc.getBidirectionalStreamMethod = getBidirectionalStreamMethod =
              io.grpc.MethodDescriptor.<io.nebula.rpc.grpc.proto.RpcRequest, io.nebula.rpc.grpc.proto.RpcResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BidirectionalStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.nebula.rpc.grpc.proto.RpcResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GenericRpcServiceMethodDescriptorSupplier("BidirectionalStream"))
              .build();
        }
      }
    }
    return getBidirectionalStreamMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GenericRpcServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericRpcServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericRpcServiceStub>() {
        @java.lang.Override
        public GenericRpcServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericRpcServiceStub(channel, callOptions);
        }
      };
    return GenericRpcServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GenericRpcServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericRpcServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericRpcServiceBlockingStub>() {
        @java.lang.Override
        public GenericRpcServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericRpcServiceBlockingStub(channel, callOptions);
        }
      };
    return GenericRpcServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GenericRpcServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GenericRpcServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GenericRpcServiceFutureStub>() {
        @java.lang.Override
        public GenericRpcServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GenericRpcServiceFutureStub(channel, callOptions);
        }
      };
    return GenericRpcServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * 通用RPC服务定义
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 一元调用
     * </pre>
     */
    default void call(io.nebula.rpc.grpc.proto.RpcRequest request,
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCallMethod(), responseObserver);
    }

    /**
     * <pre>
     * 服务器流式调用
     * </pre>
     */
    default void serverStream(io.nebula.rpc.grpc.proto.RpcRequest request,
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getServerStreamMethod(), responseObserver);
    }

    /**
     * <pre>
     * 客户端流式调用
     * </pre>
     */
    default io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcRequest> clientStream(
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getClientStreamMethod(), responseObserver);
    }

    /**
     * <pre>
     * 双向流式调用
     * </pre>
     */
    default io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcRequest> bidirectionalStream(
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getBidirectionalStreamMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service GenericRpcService.
   * <pre>
   * 通用RPC服务定义
   * </pre>
   */
  public static abstract class GenericRpcServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return GenericRpcServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service GenericRpcService.
   * <pre>
   * 通用RPC服务定义
   * </pre>
   */
  public static final class GenericRpcServiceStub
      extends io.grpc.stub.AbstractAsyncStub<GenericRpcServiceStub> {
    private GenericRpcServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericRpcServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericRpcServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 一元调用
     * </pre>
     */
    public void call(io.nebula.rpc.grpc.proto.RpcRequest request,
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCallMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 服务器流式调用
     * </pre>
     */
    public void serverStream(io.nebula.rpc.grpc.proto.RpcRequest request,
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getServerStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 客户端流式调用
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcRequest> clientStream(
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getClientStreamMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * 双向流式调用
     * </pre>
     */
    public io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcRequest> bidirectionalStream(
        io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getBidirectionalStreamMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service GenericRpcService.
   * <pre>
   * 通用RPC服务定义
   * </pre>
   */
  public static final class GenericRpcServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<GenericRpcServiceBlockingStub> {
    private GenericRpcServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericRpcServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericRpcServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 一元调用
     * </pre>
     */
    public io.nebula.rpc.grpc.proto.RpcResponse call(io.nebula.rpc.grpc.proto.RpcRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCallMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 服务器流式调用
     * </pre>
     */
    public java.util.Iterator<io.nebula.rpc.grpc.proto.RpcResponse> serverStream(
        io.nebula.rpc.grpc.proto.RpcRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getServerStreamMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service GenericRpcService.
   * <pre>
   * 通用RPC服务定义
   * </pre>
   */
  public static final class GenericRpcServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<GenericRpcServiceFutureStub> {
    private GenericRpcServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GenericRpcServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GenericRpcServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 一元调用
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.nebula.rpc.grpc.proto.RpcResponse> call(
        io.nebula.rpc.grpc.proto.RpcRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCallMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CALL = 0;
  private static final int METHODID_SERVER_STREAM = 1;
  private static final int METHODID_CLIENT_STREAM = 2;
  private static final int METHODID_BIDIRECTIONAL_STREAM = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CALL:
          serviceImpl.call((io.nebula.rpc.grpc.proto.RpcRequest) request,
              (io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse>) responseObserver);
          break;
        case METHODID_SERVER_STREAM:
          serviceImpl.serverStream((io.nebula.rpc.grpc.proto.RpcRequest) request,
              (io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CLIENT_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.clientStream(
              (io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse>) responseObserver);
        case METHODID_BIDIRECTIONAL_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.bidirectionalStream(
              (io.grpc.stub.StreamObserver<io.nebula.rpc.grpc.proto.RpcResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getCallMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.nebula.rpc.grpc.proto.RpcRequest,
              io.nebula.rpc.grpc.proto.RpcResponse>(
                service, METHODID_CALL)))
        .addMethod(
          getServerStreamMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              io.nebula.rpc.grpc.proto.RpcRequest,
              io.nebula.rpc.grpc.proto.RpcResponse>(
                service, METHODID_SERVER_STREAM)))
        .addMethod(
          getClientStreamMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              io.nebula.rpc.grpc.proto.RpcRequest,
              io.nebula.rpc.grpc.proto.RpcResponse>(
                service, METHODID_CLIENT_STREAM)))
        .addMethod(
          getBidirectionalStreamMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              io.nebula.rpc.grpc.proto.RpcRequest,
              io.nebula.rpc.grpc.proto.RpcResponse>(
                service, METHODID_BIDIRECTIONAL_STREAM)))
        .build();
  }

  private static abstract class GenericRpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GenericRpcServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.nebula.rpc.grpc.proto.RpcCommonProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GenericRpcService");
    }
  }

  private static final class GenericRpcServiceFileDescriptorSupplier
      extends GenericRpcServiceBaseDescriptorSupplier {
    GenericRpcServiceFileDescriptorSupplier() {}
  }

  private static final class GenericRpcServiceMethodDescriptorSupplier
      extends GenericRpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    GenericRpcServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GenericRpcServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GenericRpcServiceFileDescriptorSupplier())
              .addMethod(getCallMethod())
              .addMethod(getServerStreamMethod())
              .addMethod(getClientStreamMethod())
              .addMethod(getBidirectionalStreamMethod())
              .build();
        }
      }
    }
    return result;
  }
}
