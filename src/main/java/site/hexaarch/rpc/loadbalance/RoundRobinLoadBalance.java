package site.hexaarch.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡算法
 *
 * @author kenyon chen
 */
public class RoundRobinLoadBalance implements LoadBalance {
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    @Override
    public InetSocketAddress select(String serviceName, List<InetSocketAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        if (addresses.size() == 1) {
            return addresses.get(0);
        }

        // 获取或初始化计数器
        AtomicInteger count = counts.computeIfAbsent(serviceName, k -> new AtomicInteger(0));

        // 原子递增并取模
        int index = count.incrementAndGet() % addresses.size();
        if (index < 0) {
            index = -index;
        }

        return addresses.get(index);
    }
}