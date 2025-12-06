package site.hexaarch.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡算法
 *
 * @author kenyon chen
 */
public class RandomLoadBalance implements LoadBalance {
    private final Random random = new Random();

    @Override
    public InetSocketAddress select(String serviceName, List<InetSocketAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        if (addresses.size() == 1) {
            return addresses.get(0);
        }

        // 随机选择一个地址
        int index = random.nextInt(addresses.size());
        return addresses.get(index);
    }
}