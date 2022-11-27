package studio.lh.transport.netty.client;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcResponse;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:42
 * @description :
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    public static final Logger LOGGER = LoggerFactory.getLogger(NettyClientHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            RpcResponse rpcResponse = (RpcResponse) msg;
            LOGGER.info(String.format("client receive msg: %s", rpcResponse));
            // 声明一个 AttributeKey 对象，类似于 Map 中的 key
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse" + rpcResponse.getRequestId());
            /*
             * AttributeMap 可以看作是一个Channel的共享数据源
             * AttributeMap 的 key 是 AttributeKey，value 是 Attribute
             */
            // 将服务端的返回结果保存到 AttributeMap 上
            ctx.channel().attr(key).set(rpcResponse);
            ctx.channel().close();
        } finally {
            // 释放引用
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("client catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
