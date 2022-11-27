package studio.lh.transport.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.serialize.Serializer;
import studio.lh.serialize.kryo.KryoSerializer;
import studio.lh.transport.netty.NettyKryoDecoder;
import studio.lh.transport.netty.NettyKryoEncoder;
import studio.lh.util.ThreadPoolFactory;

import java.util.concurrent.ExecutorService;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 21:10
 * @description :
 */
public class NettyRpcServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcServer.class);
    private final int port;
    private Serializer kryoSerializer;


    public NettyRpcServer(int port) {
        this.port = port;
        kryoSerializer = new KryoSerializer();
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // RpcRequest的解码器
                            ch.pipeline().addLast(new NettyKryoDecoder(kryoSerializer, RpcRequest.class));
                            // RpcResponse的编码器
                            ch.pipeline().addLast(new NettyKryoEncoder(kryoSerializer, RpcResponse.class));
                            // 业务的
                            ch.pipeline().addLast(new NettyServerHandler());
                        }
                    })
                    // 设置tcp缓冲区
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            // 绑定端口，同步等待绑定成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("occur exception when start server:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
