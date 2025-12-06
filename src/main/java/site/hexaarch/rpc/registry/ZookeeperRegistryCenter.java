package site.hexaarch.rpc.registry;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 使用ZooKeeper实现的服务注册中心
 *
 * @author kenyon chen
 */
public class ZookeeperRegistryCenter implements RegistryCenter {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);

    private static final String ROOT_PATH = "/rpc-registry";
    private static final int DEFAULT_SESSION_TIMEOUT = 30000;
    private final ZooKeeper zooKeeper;
    private final Map<String, List<ServiceChangeListener>> listeners = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param connectString ZooKeeper连接字符串
     */
    public ZookeeperRegistryCenter(String connectString) throws Exception {
        this(connectString, DEFAULT_SESSION_TIMEOUT);
    }

    /**
     * 构造函数
     *
     * @param connectString  ZooKeeper连接字符串
     * @param sessionTimeout 会话超时时间
     */
    public ZookeeperRegistryCenter(String connectString, int sessionTimeout) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        // 创建ZooKeeper连接
        this.zooKeeper = new ZooKeeper(connectString, sessionTimeout, event -> {
            if (event.getType() == Watcher.Event.EventType.None) {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                } else if (event.getState() == Watcher.Event.KeeperState.Expired) {
                    logger.error("ZooKeeper session expired");
                }
            }
        });

        // 等待连接建立
        if (!latch.await(sessionTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Failed to connect to ZooKeeper within timeout");
        }

        // 确保根节点存在
        ensurePathExists(ROOT_PATH);
    }

    @Override
    public void register(String serviceName, InetSocketAddress address) throws Exception {
        // 创建服务节点
        String servicePath = ROOT_PATH + "/" + serviceName;
        ensurePathExists(servicePath);

        // 创建临时节点，服务下线后自动删除
        String addressPath = servicePath + "/" + address.toString().replace("/", "_");
        zooKeeper.create(addressPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        logger.debug("Registered service: {} at {}", serviceName, address);
    }

    @Override
    public void unregister(String serviceName, InetSocketAddress address) throws Exception {
        String addressPath = ROOT_PATH + "/" + serviceName + "/" + address.toString().replace("/", "_");
        try {
            zooKeeper.delete(addressPath, -1);
            logger.debug("Unregistered service: {} at {}", serviceName, address);
        } catch (KeeperException.NoNodeException e) {
            // 节点不存在，忽略
            logger.debug("Service node not found: {}", addressPath);
        }
    }

    @Override
    public List<InetSocketAddress> discover(String serviceName) throws Exception {
        String servicePath = ROOT_PATH + "/" + serviceName;
        List<String> children = zooKeeper.getChildren(servicePath, new ServiceNodeWatcher(serviceName));

        List<InetSocketAddress> addresses = new ArrayList<>();
        for (String child : children) {
            String[] parts = child.replace("_", "/").split(":");
            addresses.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
        }

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
            if (zooKeeper != null) {
                zooKeeper.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 确保路径存在
     */
    private void ensurePathExists(String path) throws Exception {
        Stat stat = zooKeeper.exists(path, false);
        if (stat == null) {
            // 递归创建路径
            String[] parts = path.split("/");
            String currentPath = "";
            for (String part : parts) {
                if (part.isEmpty()) continue;
                currentPath += "/" + part;
                if (zooKeeper.exists(currentPath, false) == null) {
                    zooKeeper.create(currentPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            }
        }
    }

    /**
     * 服务节点监听器
     */
    private class ServiceNodeWatcher implements Watcher {
        private final String serviceName;

        public ServiceNodeWatcher(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void process(WatchedEvent event) {
            if (event.getType() == Event.EventType.NodeChildrenChanged) {
                try {
                    // 重新获取服务列表并触发监听器
                    List<InetSocketAddress> addresses = discover(serviceName);
                    List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
                    if (serviceListeners != null) {
                        for (ServiceChangeListener listener : serviceListeners) {
                            listener.onServiceChange(serviceName, addresses);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error discovering services", e);
                }
            }
        }
    }
}