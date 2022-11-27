package studio.lh.transport.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import studio.lh.serialize.Serializer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:06
 * @description : 重写MessageToByteEncoder，所以可以作为出站处理器
 */
@AllArgsConstructor
public class NettyKryoEncoder extends MessageToByteEncoder<Object> {
    private Serializer serializer;
    private Class<?> genericClass;

    /**
     * 将对象转换为字节码然后写入到 ByteBuf 对象中
     * LD格式
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) {
        // 这个方法是Java语言instanceof操作符的动态等效。
        // 形参: Obj -要检查的对象 返回值: 如果obj是该类的实例，则为True
        // 如果收到的object不是指定类型的，就无需编码。指定类型由构造NettyKryoEncoder时指定
        if (genericClass.isInstance(o)) {
            // 1. 将对象转换为byte
            byte[] body = serializer.serialize(o);
            // 2. 读取消息的长度
            int dataLength = body.length;
            // 3.写入消息对应的字节数组长度,writerIndex 加 4
            byteBuf.writeInt(dataLength);
            //4.将字节数组写入 ByteBuf 对象中
            byteBuf.writeBytes(body);
        }
    }
}
