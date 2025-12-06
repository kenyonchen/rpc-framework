package site.hexaarch.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡接口
 *
 * @author kenyon chen
 */
public interface LoadBalance {
    /**
     * 从服务地址列表中选择一个地址
     *
     * @param serviceName 服务名称
     * @param addresses   服务地址列表
     * @return 选中的服务地址
     */
    InetSocketAddress select(String serviceName, List<InetSocketAddress> addresses);
}