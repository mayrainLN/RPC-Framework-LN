package studio.lh.transport.netty.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.transport.RpcRequestHandler;
import studio.lh.factory.ThreadPoolFactory;

import java.util.concurrent.ExecutorService;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 20:57
 * @description :
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerHandler.class);

    private static final String THREAD_NAME_PREFIX = "netty-server-handler";

    // 服务动态调用者
    private final RpcRequestHandler rpcRequestHandler;

    // 执行方法的线程池
    private final ExecutorService threadPool;

    public NettyServerHandler() {
        rpcRequestHandler = new RpcRequestHandler();
        threadPool = ThreadPoolFactory.createDefaultThreadPool(THREAD_NAME_PREFIX);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 交由自定义的线程池netty-server-handler 去执行业务
        threadPool.execute(() -> {
            try {
                LOGGER.info("服务器接收到请求: {}", msg);
                Object result = rpcRequestHandler.handle((RpcRequest) msg);
                // 业务处理完，返回结果
                if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                    ctx.writeAndFlush(RpcResponse.success(result, ((RpcRequest) msg).getRequestId()));
                } else {
                    LOGGER.error("通道不可写");
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
