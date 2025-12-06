package site.hexaarch.rpc.server;

import site.hexaarch.rpc.annotation.RpcService;
import site.hexaarch.rpc.registry.RegistryCenter;
import site.hexaarch.rpc.transport.NettyTransportServer;
import site.hexaarch.rpc.transport.TransportServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RPC服务端核心类，负责启动服务、注册服务和处理客户端请求
 *
 * @author kenyon chen
 */
public class RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final TransportServer transportServer;
    private final RegistryCenter registryCenter;
    private final ServiceRegistry serviceRegistry;
    private final int port;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Set<String> registeredServices = new HashSet<>();

    /**
     * 构造函数
     *
     * @param port           服务端口
     * @param registryCenter 服务注册中心
     */
    public RpcServer(int port, RegistryCenter registryCenter) {
        this.port = port;
        this.registryCenter = registryCenter;
        this.serviceRegistry = new ServiceRegistry();
        this.transportServer = new NettyTransportServer();
        logger.debug("RpcServer created with registryCenter: {}", registryCenter);
    }

    /**
     * 注册服务
     *
     * @param serviceImpl 服务实现对象
     */
    public void registerService(Object serviceImpl) {
        Class<?> serviceClass = serviceImpl.getClass();

        // 检查是否有RpcService注解
        if (serviceClass.isAnnotationPresent(RpcService.class)) {
            RpcService rpcService = serviceClass.getAnnotation(RpcService.class);
            Class<?> interfaceClass = rpcService.value();
            String serviceName = interfaceClass.getName();

            // 注册服务
            serviceRegistry.registerService(serviceName, serviceImpl);
            registeredServices.add(serviceName);

            logger.debug("Registered service: {} with implementation: {}", serviceName, serviceImpl.getClass().getName());
        } else {
            throw new IllegalArgumentException("Service implementation must be annotated with @RpcService");
        }
    }

    /**
     * 注册服务
     *
     * @param serviceClass 服务接口类
     * @param serviceImpl  服务实现对象
     */
    public void registerService(Class<?> serviceClass, Object serviceImpl) {
        if (!serviceClass.isAssignableFrom(serviceImpl.getClass())) {
            throw new IllegalArgumentException("Service implementation must implement the service interface");
        }

        String serviceName = serviceClass.getName();
        serviceRegistry.registerService(serviceName, serviceImpl);
        registeredServices.add(serviceName);

        logger.debug("Registered service: {} with implementation: {}", serviceName, serviceImpl.getClass().getName());
    }

    /**
     * 启动服务
     */
    public void start() throws Exception {
        logger.debug("Starting RpcServer...");
        if (!started.compareAndSet(false, true)) {
            logger.debug("Server already started, returning.");
            return;
        }

        try {
            // 启动网络传输服务
            logger.debug("Starting transport server on port: {}", port);
            transportServer.start(port, new RpcRequestHandler(serviceRegistry));
            logger.debug("RPC server started on port: {}", port);
        } catch (Exception e) {
            logger.error("Failed to start transport server", e);
            throw e;
        }

        // 注册服务到服务中心
        logger.debug("Registry center: {}", registryCenter);
        logger.debug("Number of registered services: {}", registeredServices.size());
        if (registryCenter != null) {
            InetSocketAddress address = new InetSocketAddress("localhost", port);
            logger.debug("Attempting to register {} services", registeredServices.size());
            for (String serviceName : registeredServices) {
                logger.debug("Registering service: {} at {}", serviceName, address);
                registryCenter.register(serviceName, address);
            }
        } else {
            logger.debug("Registry center is null, skipping service registration");
        }
    }

    /**
     * 停止服务
     */
    public void stop() throws Exception {
        if (!started.compareAndSet(true, false)) {
            return;
        }

        // 从服务中心注销服务
        if (registryCenter != null) {
            InetSocketAddress address = new InetSocketAddress("localhost", port);
            for (String serviceName : registeredServices) {
                registryCenter.unregister(serviceName, address);
            }
            registryCenter.close();
        }

        // 停止网络传输服务
        transportServer.stop();
        logger.debug("RPC server stopped");
    }

    /**
     * 获取服务端口
     *
     * @return 服务端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 服务是否已启动
     *
     * @return 是否已启动
     */
    public boolean isStarted() {
        return started.get();
    }
}