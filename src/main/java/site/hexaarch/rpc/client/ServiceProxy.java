package site.hexaarch.rpc.client;

import site.hexaarch.rpc.codec.JsonSerializer;
import site.hexaarch.rpc.codec.Serializer;
import site.hexaarch.rpc.common.RpcRequest;
import site.hexaarch.rpc.common.RpcResponse;
import site.hexaarch.rpc.loadbalance.LoadBalance;
import site.hexaarch.rpc.loadbalance.RandomLoadBalance;
import site.hexaarch.rpc.registry.RegistryCenter;
import site.hexaarch.rpc.transport.NettyTransportClient;
import site.hexaarch.rpc.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

/**
 * 服务代理类，用于拦截服务方法调用并发送RPC请求
 *
 * @author kenyon chen
 */
public class ServiceProxy implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServiceProxy.class);

    private final Class<?> serviceClass;
    private final RegistryCenter registryCenter;
    private final LoadBalance loadBalance;

    /**
     * 构造函数
     *
     * @param serviceClass   服务接口类
     * @param registryCenter 服务注册中心
     */
    public ServiceProxy(Class<?> serviceClass, RegistryCenter registryCenter) {
        this(serviceClass, registryCenter, new RandomLoadBalance());
    }

    /**
     * 构造函数
     *
     * @param serviceClass   服务接口类
     * @param registryCenter 服务注册中心
     * @param loadBalance    负载均衡策略
     */
    public ServiceProxy(Class<?> serviceClass, RegistryCenter registryCenter, LoadBalance loadBalance) {
        this.serviceClass = serviceClass;
        this.registryCenter = registryCenter;
        this.loadBalance = loadBalance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 创建RPC请求
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setServiceName(serviceClass.getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);

        logger.debug("Sending RPC request: {}, service: {}, method: {}",
                request.getRequestId(), request.getServiceName(), request.getMethodName());

        // 从注册中心获取服务地址
        List<InetSocketAddress> addresses = registryCenter.discover(serviceClass.getName());
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("No service available for: " + serviceClass.getName());
        }

        // 使用负载均衡策略选择服务地址
        InetSocketAddress address = loadBalance.select(serviceClass.getName(), addresses);
        if (address == null) {
            throw new RuntimeException("No service address selected for: " + serviceClass.getName());
        }

        logger.debug("Selected service address: {}", address);

        // 创建客户端并发送请求
        TransportClient client = new NettyTransportClient();
        try {
            // 创建序列化器
            Serializer serializer = new JsonSerializer();

            // 连接到服务端
            client.connect(address);

            // 序列化请求
            byte[] requestData = serializer.serialize(request);

            // 发送请求并获取响应数据
            byte[] responseData = client.send(requestData);

            // 反序列化响应
            RpcResponse response = serializer.deserialize(responseData, RpcResponse.class);

            if (response.isSuccess()) {
                return response.getResult();
            } else {
                throw new RuntimeException("RPC call failed: " + response.getError());
            }
        } finally {
            client.close();
        }
    }

    /**
     * 创建服务代理实例
     *
     * @param serviceClass   服务接口类
     * @param registryCenter 服务注册中心
     * @param <T>            服务接口类型
     * @return 服务代理实例
     */
    public static <T> T createProxy(Class<T> serviceClass, RegistryCenter registryCenter) {
        return serviceClass.cast(Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                new ServiceProxy(serviceClass, registryCenter)
        ));
    }

    /**
     * 创建服务代理实例
     *
     * @param serviceClass   服务接口类
     * @param registryCenter 服务注册中心
     * @param loadBalance    负载均衡策略
     * @param <T>            服务接口类型
     * @return 服务代理实例
     */
    public static <T> T createProxy(Class<T> serviceClass, RegistryCenter registryCenter, LoadBalance loadBalance) {
        return serviceClass.cast(Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                new ServiceProxy(serviceClass, registryCenter, loadBalance)
        ));
    }
}