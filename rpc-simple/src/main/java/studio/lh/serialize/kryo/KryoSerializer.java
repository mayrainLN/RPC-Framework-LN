package studio.lh.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.exception.SerializeException;
import studio.lh.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 20:00
 * @description :
 * Kryo 不是线程安全的。每个线程都应该有自己的 Kryo 对象、输入和输出实例。
 * 因此在多线程环境中，可以考虑使用 ThreadLocal 或者对象池来保证线程安全性。
 * ThreadLocal 牺牲空间换取线程安全 ：为每个线程都单独创建本线程专用的 kryo 对象。对于每条线程的每个 kryo 对象来说，都是顺序执行的，因此天然避免了并发安全问题。
 */
public class KryoSerializer implements Serializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KryoSerializer.class);

    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        // 一个ThreadLocal容器中只会有一个kryo实例
        Kryo kryo = new Kryo();
        // 为了提供性能和减小序列化结果体积，提供注册的序列化对象类的方式。
        // 在注册时，会为该序列化类生成 int ID，后续在序列化时使用 int ID 唯一标识该类型
        kryo.register(RpcResponse.class);
        kryo.register(RpcRequest.class);

        kryo.setReferences(false); // 关闭循环引用检测,从而提高一些性能。
        /**
         * 注册会给每一个class一个int类型的Id相关联，这显然比类名称高效
         * 但同时要求反序列化的时候的Id必须与序列化过程中一致。这意味着注册的顺序非常重要。
         */
//        kryo.setRegistrationRequired(false); // 关闭注册行为？？？？？？？？？？？？？？？
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            //将对象序列化为byte数组
            kryo.writeObject(output, obj);
            // kryoThreadLocal调用完之后要remove一下, 防止内存泄露
            KRYO_THREAD_LOCAL.remove();
            return output.toBytes();
        } catch (Exception e) {
            LOGGER.error("occur exception when serialize:", e);
            throw new SerializeException("序列化失败");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            // 从byte数组中反序列化出对对象
            Object o = kryo.readObject(input, clazz);
            KRYO_THREAD_LOCAL.remove();
            // clazz.cast(o) 通过类对象，将o强类型转换
            return clazz.cast(o);
        } catch (Exception e) {
            LOGGER.error("occur exception when deserialize:", e);
            throw new SerializeException("反序列化失败");
        }
    }
}
