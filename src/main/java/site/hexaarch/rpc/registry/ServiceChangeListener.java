package site.hexaarch.rpc.registry;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务变化监听器接口
 *
 * @author kenyon chen
 */
public interface ServiceChangeListener {
    /**
     * 当服务实例发生变化时回调
     *
     * @param serviceName 服务名称
     * @param addresses   服务地址列表
     */
    void onServiceChange(String serviceName, List<InetSocketAddress> addresses);
}