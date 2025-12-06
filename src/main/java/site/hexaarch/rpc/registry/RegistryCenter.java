package site.hexaarch.rpc.registry;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务注册中心接口
 *
 * @author kenyon chen
 */
public interface RegistryCenter {
    /**
     * 注册服务
     *
     * @param serviceName 服务名称
     * @param address     服务地址
     * @throws Exception 注册异常
     */
    void register(String serviceName, InetSocketAddress address) throws Exception;

    /**
     * 注销服务
     *
     * @param serviceName 服务名称
     * @param address     服务地址
     * @throws Exception 注销异常
     */
    void unregister(String serviceName, InetSocketAddress address) throws Exception;

    /**
     * 发现服务
     *
     * @param serviceName 服务名称
     * @return 服务地址列表
     * @throws Exception 发现异常
     */
    List<InetSocketAddress> discover(String serviceName) throws Exception;

    /**
     * 订阅服务变化
     *
     * @param serviceName 服务名称
     * @param listener    服务变化监听器
     * @throws Exception 订阅异常
     */
    void subscribe(String serviceName, ServiceChangeListener listener) throws Exception;

    /**
     * 取消订阅
     *
     * @param serviceName 服务名称
     * @param listener    服务变化监听器
     * @throws Exception 取消订阅异常
     */
    void unsubscribe(String serviceName, ServiceChangeListener listener) throws Exception;

    /**
     * 关闭注册中心连接
     */
    void close();
}