package site.hexaarch.rpc.server;

import site.hexaarch.rpc.codec.JsonSerializer;
import site.hexaarch.rpc.codec.Serializer;
import site.hexaarch.rpc.common.RpcRequest;
import site.hexaarch.rpc.common.RpcResponse;
import site.hexaarch.rpc.transport.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * RPC请求处理器，负责接收客户端请求并调用相应的服务方法
 *
 * @author kenyon chen
 */
public class RpcRequestHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);

    private final ServiceRegistry serviceRegistry;
    private final Serializer serializer;

    /**
     * 构造函数
     *
     * @param serviceRegistry 服务注册表
     */
    public RpcRequestHandler(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.serializer = new JsonSerializer();
    }

    @Override
    public byte[] handle(byte[] requestData) {
        try {
            // 反序列化请求数据
            RpcRequest rpcRequest = serializer.deserialize(requestData, RpcRequest.class);
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setRequestId(rpcRequest.getRequestId());

            try {
                // 获取服务实现对象
                Object serviceImpl = serviceRegistry.getService(rpcRequest.getServiceName());
                if (serviceImpl == null) {
                    throw new RuntimeException("Service not found: " + rpcRequest.getServiceName());
                }

                // 获取方法并调用
                Class<?> serviceClass = serviceImpl.getClass();
                Method method = serviceClass.getMethod(
                        rpcRequest.getMethodName(),
                        rpcRequest.getParameterTypes()
                );

                // 调用方法并获取结果
                Object result = method.invoke(serviceImpl, rpcRequest.getParameters());
                rpcResponse.setResult(result);
                // 成功时设置error为null
                rpcResponse.setError(null);

                logger.info("Handled request: {}, service: {}, method: {}",
                        rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethodName());

            } catch (Exception e) {
                rpcResponse.setError(e.getMessage());
                logger.error("Error handling request: {}", rpcRequest.getRequestId(), e);
            }

            // 序列化响应数据
            return serializer.serialize(rpcResponse);
        } catch (Exception e) {
            logger.error("Error handling request", e);
            // 处理序列化异常
            RpcResponse errorResponse = new RpcResponse();
            errorResponse.setError("Failed to handle request: " + e.getMessage());
            try {
                return serializer.serialize(errorResponse);
            } catch (Exception ex) {
                logger.error("Error serializing error response", ex);
                return new byte[0];
            }
        }
    }
}