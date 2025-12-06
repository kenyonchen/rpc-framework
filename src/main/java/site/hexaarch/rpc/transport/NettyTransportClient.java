package site.hexaarch.rpc.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 使用Netty实现的网络传输客户端
 *
 * @author kenyon chen
 */
public class NettyTransportClient implements TransportClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyTransportClient.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private Channel channel;
    private EventLoopGroup group;
    private ResponseHandler responseHandler;

    @Override
    public void connect(InetSocketAddress address) {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();

        try {
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, DEFAULT_CONNECT_TIMEOUT)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 处理粘包问题
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            // 字节数组编解码器
                            pipeline.addLast(new ByteArrayDecoder());
                            pipeline.addLast(new ByteArrayEncoder());
                            // 客户端处理器
                            NettyClientHandler clientHandler = new NettyClientHandler();
                            pipeline.addLast(clientHandler);
                        }
                    });

            // 连接服务端
            ChannelFuture future = bootstrap.connect(address).sync();
            this.channel = future.channel();

            // 初始化响应处理器
            responseHandler = new ResponseHandler();
            // 设置客户端处理器的响应处理器
            ((NettyClientHandler) channel.pipeline().last()).setResponseHandler(responseHandler);
        } catch (Exception e) {
            logger.error("Failed to connect to server: {}", address, e);
            throw new RuntimeException("Failed to connect to server: " + address, e);
        }
    }

    @Override
    public byte[] send(byte[] data) throws Exception {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Channel is not connected");
        }

        // 发送数据
        channel.writeAndFlush(data).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                Throwable cause = future.cause();
                if (cause instanceof Exception) {
                    responseHandler.setException((Exception) cause);
                } else {
                    responseHandler.setException(new RuntimeException(cause));
                }
            }
        });

        // 等待响应
        return responseHandler.waitForResponse();
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    /**
     * 客户端处理器
     */
    private static class NettyClientHandler extends SimpleChannelInboundHandler<byte[]> {
        private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

        private ResponseHandler responseHandler;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
            if (responseHandler != null) {
                responseHandler.setResponse(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("Exception in Netty client handler", cause);
            if (responseHandler != null) {
                if (cause instanceof Exception) {
                    responseHandler.setException((Exception) cause);
                } else {
                    responseHandler.setException(new RuntimeException(cause));
                }
            }
            ctx.close();
        }

        public void setResponseHandler(ResponseHandler responseHandler) {
            this.responseHandler = responseHandler;
        }
    }

    /**
     * 响应处理器
     */
    private static class ResponseHandler {
        private final CountDownLatch latch = new CountDownLatch(1);
        private byte[] response;
        private Exception exception;

        public byte[] waitForResponse() throws Exception {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Request timeout");
            }
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        public void setResponse(byte[] response) {
            this.response = response;
            latch.countDown();
        }

        public void setException(Exception exception) {
            this.exception = exception;
            latch.countDown();
        }
    }
}