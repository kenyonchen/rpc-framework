package site.hexaarch.rpc.transport;

/**
 * 网络传输服务端接口
 *
 * @author kenyon chen
 */
public interface TransportServer {
    /**
     * 启动服务端
     *
     * @param port    服务端口
     * @param handler 请求处理器
     */
    void start(int port, RequestHandler handler);

    /**
     * 停止服务端
     */
    void stop();

    /**
     * 获取服务端口
     *
     * @return 服务端口
     */
    int getPort();
}