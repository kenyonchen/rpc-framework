package site.hexaarch.rpc.transport;

/**
 * 请求处理器接口
 *
 * @author kenyon chen
 */
public interface RequestHandler {
    /**
     * 处理请求
     *
     * @param request 请求数据
     * @return 响应数据
     */
    byte[] handle(byte[] request);
}