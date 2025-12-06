package site.hexaarch.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务注册表，用于存储和管理服务实现类
 *
 * @author kenyon chen
 */
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    /**
     * 注册服务
     *
     * @param serviceName 服务名称
     * @param serviceImpl 服务实现对象
     */
    public void registerService(String serviceName, Object serviceImpl) {
        serviceMap.put(serviceName, serviceImpl);
        logger.debug("Registered service: " + serviceName);
    }

    /**
     * 注册服务
     *
     * @param serviceClass 服务接口类
     * @param serviceImpl  服务实现对象
     */
    public void registerService(Class<?> serviceClass, Object serviceImpl) {
        registerService(serviceClass.getName(), serviceImpl);
    }

    /**
     * 获取服务实现对象
     *
     * @param serviceName 服务名称
     * @return 服务实现对象
     */
    public Object getService(String serviceName) {
        return serviceMap.get(serviceName);
    }

    /**
     * 获取服务实现对象
     *
     * @param serviceClass 服务接口类
     * @return 服务实现对象
     */
    public <T> T getService(Class<T> serviceClass) {
        return serviceClass.cast(getService(serviceClass.getName()));
    }

    /**
     * 获取所有服务名称
     *
     * @return 服务名称集合
     */
    public Map<String, Object> getAllServices() {
        return serviceMap;
    }
}