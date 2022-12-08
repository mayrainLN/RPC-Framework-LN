package studio.lh.transport.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;
import studio.lh.loadbalancer.LoadBalancer;
import studio.lh.loadbalancer.RoundRobinLoadBalancer;
import studio.lh.registry.NacosServiceDiscovery;
import studio.lh.registry.ServiceDiscovery;
import studio.lh.serialize.Serializer;
import studio.lh.transport.RpcClient;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:51
 * @description :
 */
public class NettyRpcClient implements RpcClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcClient.class);

    private final Serializer serializer;

    private static final EventLoopGroup GROUP;

    private static final Bootstrap BOOTSTRAP;

    private static final int DEFAULT_SERIALIZER_CODE = 0;

    /**
     * 存放客户端尚未得到响应的请求
     */
    private final UnprocessedRequests unprocessedRequests;
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
    }

    // 默认使用Kryo序列化,轮询负载均衡
    public NettyRpcClient() {
        this(DEFAULT_SERIALIZER_CODE, new RoundRobinLoadBalancer());
    }

    public NettyRpcClient(int code, LoadBalancer loadBalancer) {
        serviceDiscovery = new NacosServiceDiscovery(loadBalancer);
        serializer = Serializer.getSerializer(code);
        unprocessedRequests = new UnprocessedRequests();
    }

    /**
     * 发送消息, 返回包装RpcResponse的CompletableFuture
     * @param rpcRequest 消息体
     * @return 服务端返回的数据
     */
    @Override
    public CompletableFuture<RpcResponse> sendRpcRequest(RpcRequest rpcRequest) {
        if (serializer == null) {
            LOGGER.error("未设置序列化器");
            throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND);
        }
        CompletableFuture<RpcResponse> resultFuture = new CompletableFuture<>();
        try {
            // 从注册中心获取服务实例地址
            InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest.getInterfaceName());
            // 获取连接到服务实例的channel
            Channel channel = null;
            try {
                channel = ChannelProvider.get(inetSocketAddress, serializer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!channel.isActive()) {
                GROUP.shutdownGracefully();
                return null;

            }
            // 记录还未被响应的请求
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            // 给writeAndFlush方法返回的ChannelFuture对象添加监听器
            channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()) {
                    LOGGER.info("客户端发送消息: {}", rpcRequest.toString());
                } else {
                    future1.channel().close();
                    resultFuture.completeExceptionally(future1.cause());
                    LOGGER.error("发送消息时有错误发生: ", future1.cause());
                }
            });
        } catch (RuntimeException e) {
            // 清除请求
            unprocessedRequests.remove(rpcRequest.getRequestId());
            LOGGER.error(e.getMessage(), e);
            /**
             * 当你捕获InterruptException并吞下它时，你基本上阻止任何更高级别的方法/线程组注意到中断。
             * 这可能会导致问题。
             * 通过调用Thread.currentThread().interrupt()
             * 设置线程的中断标志，因此更高级别的中断处理程序会注意到它并且可以正确处理它。
             */
            Thread.currentThread().interrupt();
        }
        // 返回future
        return resultFuture;
    }
}
