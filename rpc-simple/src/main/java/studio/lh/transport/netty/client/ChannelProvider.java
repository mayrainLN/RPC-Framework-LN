package studio.lh.transport.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;
import studio.lh.serialize.Serializer;
import studio.lh.transport.netty.NettyKryoDecoder;
import studio.lh.transport.netty.NettyKryoEncoder;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/28 12:21
 * @description :
 */
public class ChannelProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelProvider.class);
    private static EventLoopGroup eventLoopGroup;
    private static Bootstrap bootstrap;
    /**
     * 默认最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 5;
    private static Channel channel = null;

    static {
        bootstrap = initializeBootstrap();
    }

    private static Bootstrap initializeBootstrap() {
        eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                //连接的超时时间，超过这个时间还是建立不上的话则代表连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                //是否开启 TCP 底层心跳机制
                .option(ChannelOption.SO_KEEPALIVE, true)
                //TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                .option(ChannelOption.TCP_NODELAY, true);
        return bootstrap;
    }


    /**
     * 获取用于发出请求的Channel
     * @param inetSocketAddress 从注册中心获取到的服务实例的地址
     * @param serializer 序列化器
     * @return 于服务提供端相连的Channel
     */
    public static Channel get(InetSocketAddress inetSocketAddress, Serializer serializer) {
        // 设置handler, 既然有了通信需求，所以就地设置编解码器
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                /*自定义序列化编解码器*/
                ch.pipeline().addLast(new NettyKryoDecoder(serializer, RpcResponse.class))
                        .addLast(new NettyKryoEncoder(serializer, RpcRequest.class))
                        .addLast(new NettyClientHandler());
            }
        });
        /**
         * CountDownLatch是一个同步工具类，用来协调多个线程之间的同步，或者说起到线程之间的通信（而不是用作互斥的作用）。
         * 能够使一个线程在等待另外一些线程完成各自工作之后，再继续执行。使用一个计数器进行实现。
         * 计数器初始值为线程的数量。当每一个线程完成自己任务后，计数器的值就会减一。
         * 当计数器的值为0时，表示所有的线程都已经完成一些任务，然后在CountDownLatch上等待的线程就可以恢复执行接下来的任务。
         */
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            /**
             * 注意：执行connect的是Nio线程，所以需要等到连接建立后才能向后执行。
             * 如果都写在一起, 直接写.sync就好了
             */
            connect(bootstrap, inetSocketAddress, countDownLatch);
            // 阻塞直到countDownLatch减为1
            countDownLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error("获取channel时有错误发生:", e);
        }
        return channel;
    }

    /**
     * 缺省重试次数时，以MAX_RETRY_COUNT为默认
     * @param bootstrap
     * @param inetSocketAddress
     * @param countDownLatch
     */
    private static void connect(Bootstrap bootstrap, InetSocketAddress inetSocketAddress, CountDownLatch countDownLatch) {
        connect(bootstrap, inetSocketAddress, MAX_RETRY_COUNT, countDownLatch);
    }

    /**
     *
     * @param bootstrap
     * @param inetSocketAddress 从注册中心获取到的服务实例的地址
     * @param retry 最大重试次数
     * @param countDownLatch
     */
    private static void connect(Bootstrap bootstrap, InetSocketAddress inetSocketAddress, int retry, CountDownLatch countDownLatch) {
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                LOGGER.info("client connected!");
                channel = future.channel();
                // 连接成功了
                countDownLatch.countDown();
                return;
            }
            if (retry == 0) {
                LOGGER.error("connect failed : over max retry！");
                countDownLatch.countDown();
                throw new RpcException(RpcErrorMessageEnum.CLIENT_CONNECT_SERVER_FAILURE);
            }
            // 第几次重连
            int order = (MAX_RETRY_COUNT - retry) + 1;
            // 本次重连的间隔递增
            int delay = 1 << order;
            LOGGER.error("{}: connect failed ：retrying for {} times……", new Date(), order);
            bootstrap.config().group().schedule(() -> connect(bootstrap, inetSocketAddress, retry - 1, countDownLatch), delay, TimeUnit
                    .SECONDS);
        });
    }
}
