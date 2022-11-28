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
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;
import studio.lh.provider.ServiceProvider;
import studio.lh.provider.ServiceProviderImpl;
import studio.lh.registry.NacosServiceRegistry;
import studio.lh.registry.ServiceRegistry;
import studio.lh.serialize.Serializer;
import studio.lh.serialize.kryo.KryoSerializer;
import studio.lh.transport.RpcServer;
import studio.lh.transport.netty.NettyKryoDecoder;
import studio.lh.transport.netty.NettyKryoEncoder;

import java.net.InetSocketAddress;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 21:10
 * @description :
 */
public class NettyRpcServer implements RpcServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcServer.class);

    private final String host;

    private final int port;

    private final ServiceRegistry serviceRegistry;

    private final ServiceProvider serviceProvider;

    private Serializer serializer;



    public NettyRpcServer(String host, int port) {
        // 默认缺省使用KryoSerializer
        serializer = new KryoSerializer();
        this.host = host;
        this.port = port;
        serviceRegistry = new NacosServiceRegistry();
        serviceProvider = new ServiceProviderImpl();
    }

    @Override
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public <T> void publishService(Object service, Class<T> serviceClass) {
        // 向外提供服务前，要先设置序列化器
        if (serializer == null) {
            LOGGER.error("未设置序列化器");
            throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND);
        }
        // 将服务注册到本地的map，键是动态获取的规范类名
        serviceProvider.addService(service);
        // 将服务注册到远程的注册中心
        serviceRegistry.register(serviceClass.getCanonicalName(), new InetSocketAddress(host, port));
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // RpcRequest的解码器
                            ch.pipeline().addLast(new NettyKryoDecoder(serializer, RpcRequest.class));
                            // RpcResponse的编码器
                            ch.pipeline().addLast(new NettyKryoEncoder(serializer, RpcResponse.class));
                            // 业务的
                            ch.pipeline().addLast(new NettyServerHandler());
                        }
                    })
                    // 设置tcp缓冲区
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            // 服务端实例不只有一个了, 所以需要用host区分
            // 绑定ip和端口，同步等待绑定成功
            ChannelFuture future = serverBootstrap.bind(host, port).sync();
            // 主线程等待服务端监听端口关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("occur exception when start server:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
