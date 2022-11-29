package studio.lh.transport.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;
import studio.lh.registry.NacosServiceDiscovery;
import studio.lh.registry.ServiceDiscovery;
import studio.lh.serialize.Serializer;
import studio.lh.serialize.kryo.KryoSerializer;
import studio.lh.transport.RpcClient;
import studio.lh.util.RpcMessageChecker;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:51
 * @description :
 */
public class NettyRpcClient implements RpcClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcClient.class);
    private static Serializer serializer;
    private static final EventLoopGroup GROUP;
    private static final Bootstrap BOOTSTRAP;
    /**
     * 远程Nacos注册中心
     */
    private final ServiceDiscovery serviceDiscovery;

    static {
        GROUP = new NioEventLoopGroup();
        BOOTSTRAP = new Bootstrap();
        BOOTSTRAP.group(GROUP)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);
        serializer = new KryoSerializer();
    }

    public NettyRpcClient() {
        this.serviceDiscovery = new NacosServiceDiscovery();
    }

    /**
     * 发送消息到服务端
     * @param rpcRequest 消息体
     * @return 服务端返回的数据
     */
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        if (serializer == null) {
            LOGGER.error("未设置序列化器");
            throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND);
        }
        /**
         * 保证在修改对象引用时的线程安全性。
         */
        AtomicReference<Object> result = new AtomicReference<>(null);
        try {
            // 从注册中心获取服务实例地址
            InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest.getInterfaceName());
            // 获取连接到服务实例的channel
            Channel channel = ChannelProvider.get(inetSocketAddress, serializer);
            if (!channel.isActive()) {
                GROUP.shutdownGracefully();
                return null;
            }
            // 写出请求
            channel.writeAndFlush(rpcRequest).addListener(future -> {
                if (future.isSuccess()) {
                    LOGGER.info("客户端发送消息: {}", rpcRequest.toString());
                } else {
                    LOGGER.error("发送消息时有错误发生: ", future.cause());
                }
            });
            // 阻塞直到Channel关闭
            channel.closeFuture().sync();
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse" + rpcRequest.getRequestId());
            RpcResponse rpcResponse = channel.attr(key).get();
            RpcMessageChecker.check(rpcResponse, rpcRequest);
            result.set(rpcResponse.getData());
        } catch (InterruptedException e) {
            LOGGER.error("发送消息时有错误发生: ", e);
        }
        return result.get();
    }
}
