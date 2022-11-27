package studio.lh.transport.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.serialize.kryo.KryoSerializer;
import studio.lh.transport.RpcClient;
import studio.lh.transport.netty.NettyKryoDecoder;
import studio.lh.transport.netty.NettyKryoEncoder;
import studio.lh.util.RpcMessageChecker;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:51
 * @description :
 */
public class NettyRpcClient implements RpcClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcClient.class);
    private String host;
    private int port;
    private static final Bootstrap BOOTSTRAP;

    public NettyRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // 初始化相关资源比如 EventLoopGroup、Bootstrap
    static {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        BOOTSTRAP = new Bootstrap();
        KryoSerializer kryoSerializer = new KryoSerializer();
        BOOTSTRAP.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                /*
                * 此为TCP传输选项，表示是否开启TCP的心跳机制。true为连接保持心跳，默认值为false。
                * 启用该功能时，TCP会主动探测空闲连接的有效性。需要注意的是：默认的心跳间隔是7200秒，即2小时。Netty默认关闭该功能。
                * */
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        /*自定义序列化编解码器*/
                        ch.pipeline().addLast(new NettyKryoDecoder(kryoSerializer, RpcResponse.class));
                        ch.pipeline().addLast(new NettyKryoEncoder(kryoSerializer, RpcRequest.class));
                        // 处理业务的Handler
                        ch.pipeline().addLast(new NettyClientHandler());
                    }
                });
    }

    /**
     * 发送消息到服务端
     *
     * @param rpcRequest 消息体
     * @return 服务端返回的数据
     */
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        try {
            // 同步等待连接建立完成
            ChannelFuture f = BOOTSTRAP.connect(host, port).sync();
            LOGGER.info("client connect  {}:{}", host, port);
            Channel futureChannel = f.channel();
            if (futureChannel != null) {
                futureChannel.writeAndFlush(rpcRequest).addListener(future -> {
                    if (future.isSuccess()) {
                        LOGGER.info(String.format("client send message: %s", rpcRequest.toString()));
                    } else {
                        LOGGER.error("Send failed:", future.cause());
                    }
                });
                // 阻塞直到channel关闭
                futureChannel.closeFuture().sync();
                AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse" + rpcRequest.getRequestId());
                // 获取rpcRequest对应的rpcResponse
                RpcResponse rpcResponse = futureChannel.attr(key).get();
                // 校验
                RpcMessageChecker.check(rpcResponse, rpcRequest);
                return rpcResponse.getData();
            }
        } catch (InterruptedException e) {
            LOGGER.error("occur exception when connect server:", e);
        }
        return null;
    }
}
