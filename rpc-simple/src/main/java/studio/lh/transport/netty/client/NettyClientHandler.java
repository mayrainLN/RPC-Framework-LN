package studio.lh.transport.netty.client;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcResponse;
import studio.lh.factory.SingletonFactory;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:42
 * @description :
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    public static final Logger LOGGER = LoggerFactory.getLogger(NettyClientHandler.class);

    private final UnprocessedRequests unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            RpcResponse rpcResponse = (RpcResponse) msg;
            // 读到响应后，更改请求的状态
            unprocessedRequests.complete(rpcResponse);
        } finally {
            // 释放引用
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("rpc客户端捕捉到异常");
        cause.printStackTrace();
        ctx.close();
    }
}
