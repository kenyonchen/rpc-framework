package site.hexaarch.rpc.registry;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 使用Nacos实现的服务注册中心
 *
 * @author kenyon chen
 */
public class NacosRegistryCenter implements RegistryCenter {
    private static final Logger logger = LoggerFactory.getLogger(NacosRegistryCenter.class);

    private final NamingService namingService;
    private final Map<String, List<ServiceChangeListener>> listeners = new ConcurrentHashMap<>();
    private final String groupName;

    /**
     * 构造函数
     *
     * @param serverAddr Nacos服务器地址
     */
    public NacosRegistryCenter(String serverAddr) throws NacosException {
        this(serverAddr, "DEFAULT_GROUP");
    }

    /**
     * 构造函数
     *
     * @param serverAddr Nacos服务器地址
     * @param groupName  组名
     */
    public NacosRegistryCenter(String serverAddr, String groupName) throws NacosException {
        this.groupName = groupName;
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        this.namingService = NacosFactory.createNamingService(properties);
    }

    @Override
    public void register(String serviceName, InetSocketAddress address) throws Exception {
        logger.debug("Registering service: {} at {} in group {}", serviceName, address, groupName);
        Instance instance = new Instance();
        instance.setIp(address.getAddress().getHostAddress());
        instance.setPort(address.getPort());
        // 设置权重
        instance.setWeight(1.0);
        // 设置为临时实例，服务下线后自动删除
        instance.setEphemeral(true);
        // 设置为健康状态
        instance.setHealthy(true);

        namingService.registerInstance(serviceName, groupName, instance);
        logger.debug("Successfully registered service: {} at {} in group {}", serviceName, address, groupName);
    }

    @Override
    public void unregister(String serviceName, InetSocketAddress address) throws Exception {
        // Nacos会在服务下线后自动移除临时实例，这里不需要手动注销
        logger.debug("Unregistering service: {} at {} (will be automatically removed)", serviceName, address);
    }

    @Override
    public List<InetSocketAddress> discover(String serviceName) throws Exception {
        logger.debug("Discovering service: {} in group {}", serviceName, groupName);
        // 获取所有健康的服务实例
        List<Instance> instances = namingService.getAllInstances(serviceName, groupName);

        logger.debug("Found {} instances for service: {}", instances.size(), serviceName);
        for (Instance instance : instances) {
            logger.debug("Instance: {}:{}", instance.getIp(), instance.getPort());
        }

        // 添加监听器以监听服务变化
        namingService.subscribe(serviceName, groupName, new NacosEventListener(serviceName));

        List<InetSocketAddress> addresses = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.isHealthy()) {
                addresses.add(new InetSocketAddress(instance.getIp(), instance.getPort()));
            }
        }

        logger.debug("Returning {} healthy addresses for service: {}", addresses.size(), serviceName);

        return addresses;
    }

    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) throws Exception {
        listeners.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(listener);
        // 立即触发一次回调，获取当前服务列表
        listener.onServiceChange(serviceName, discover(serviceName));
    }

    @Override
    public void unsubscribe(String serviceName, ServiceChangeListener listener) throws Exception {
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        if (serviceListeners != null) {
            serviceListeners.remove(listener);
        }
    }

    @Override
    public void close() {
        try {
            if (namingService != null) {
                namingService.shutDown();
            }
        } catch (Exception e) {
            logger.error("Error closing Nacos naming service", e);
        }
    }

    /**
     * Nacos事件监听器
     */
    private class NacosEventListener implements EventListener {
        private final String serviceName;

        public NacosEventListener(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                NamingEvent namingEvent = (NamingEvent) event;
                List<Instance> instances = namingEvent.getInstances();

                List<InetSocketAddress> addresses = instances.stream()
                        .filter(Instance::isHealthy)
                        .map(instance -> new InetSocketAddress(instance.getIp(), instance.getPort()))
                        .collect(Collectors.toList());

                // 触发监听器
                List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
                if (serviceListeners != null) {
                    for (ServiceChangeListener listener : serviceListeners) {
                        try {
                            listener.onServiceChange(serviceName, addresses);
                        } catch (Exception e) {
                            logger.error("Error notifying service change listener", e);
                        }
                    }
                }
            }
        }
    }
}