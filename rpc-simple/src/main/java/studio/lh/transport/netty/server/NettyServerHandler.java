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
import studio.lh.registry.DefaultServiceRegistry;
import studio.lh.registry.ServiceRegistry;
import studio.lh.transport.RpcRequestHandler;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 20:57
 * @description :
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerHandler.class);
    // 服务动态调用者
    private static RpcRequestHandler rpcRequestHandler;
    private static ServiceRegistry serviceRegistry;
    static {
        rpcRequestHandler = new RpcRequestHandler();
        serviceRegistry = new DefaultServiceRegistry();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            RpcRequest rpcRequest = (RpcRequest) msg;
            LOGGER.info(String.format("server receive msg: %s", rpcRequest));
            String interfaceName = rpcRequest.getInterfaceName();
            // 从注册中心获取服务实现
            Object service = serviceRegistry.getService(interfaceName);
            // 交由给rpcRequestHandler去反射调用方法
            Object result = rpcRequestHandler.handle(rpcRequest, service);
            LOGGER.info(String.format("server get result: %s", result.toString()));
            // 写会调用结果
            ChannelFuture f = ctx.writeAndFlush(RpcResponse.success(result, rpcRequest.getRequestId()));
            // 写会完成后关闭channel
            f.addListener(ChannelFutureListener.CLOSE);
        } finally {
            /*
            * ReferenceCountUtil.release()其实是ByteBuf.release()方法（从ReferenceCounted接口继承而来）的包装。
            * 从InBound里读取的ByteBuf要手动释放，还有自己创建的ByteBuf要自己负责释放。这两处要调用这个release方法。
            * write Bytebuf到OutBound时由netty负责释放，不需要手动调用release
            * */
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
