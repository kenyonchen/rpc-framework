package site.hexaarch.rpc.transport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 使用Netty实现的网络传输服务端
 *
 * @author kenyon chen
 */
public class NettyTransportServer implements TransportServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyTransportServer.class);

    private static final int DEFAULT_PORT = 8888;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private int port;

    @Override
    public void start(int port, RequestHandler handler) {
        this.port = port > 0 ? port : DEFAULT_PORT;

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 处理粘包问题
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            // 字节数组编解码器
                            pipeline.addLast(new ByteArrayDecoder());
                            pipeline.addLast(new ByteArrayEncoder());
                            // 服务端处理器
                            pipeline.addLast(new NettyServerHandler(handler));
                        }
                    });

            // 启动服务
            ChannelFuture future = bootstrap.bind(this.port).sync();
            serverChannel = future.channel();
            logger.info("RPC Server started on port: {}", this.port);

            // 不再等待服务关闭，让程序继续执行
            // serverChannel.closeFuture().sync();
        } catch (Exception e) {
            logger.error("Failed to start server on port: {}", this.port, e);
            throw new RuntimeException("Failed to start server on port: " + this.port, e);
        }
        // 移除了finally块中的stop()调用，避免服务意外停止
    }

    @Override
    public void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            logger.info("RPC Server stopped");
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    /**
     * 服务端处理器
     */
    private static class NettyServerHandler extends SimpleChannelInboundHandler<byte[]> {
        private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

        private final RequestHandler requestHandler;

        public NettyServerHandler(RequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] request) throws Exception {
            // 处理请求
            byte[] response = requestHandler.handle(request);
            // 发送响应
            ctx.writeAndFlush(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("Exception in Netty server handler", cause);
            ctx.close();
        }
    }
}