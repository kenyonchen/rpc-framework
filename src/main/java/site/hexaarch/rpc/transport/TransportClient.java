package site.hexaarch.rpc.transport;

import java.net.InetSocketAddress;

/**
 * 网络传输客户端接口
 *
 * @author kenyon chen
 */
public interface TransportClient {
    /**
     * 连接到服务端
     *
     * @param address 服务端地址
     */
    void connect(InetSocketAddress address);

    /**
     * 发送数据
     *
     * @param data 要发送的数据
     * @return 服务端响应的数据
     * @throws Exception 发送异常
     */
    byte[] send(byte[] data) throws Exception;

    /**
     * 关闭连接
     */
    void close();
}