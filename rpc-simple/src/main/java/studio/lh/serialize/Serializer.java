package studio.lh.serialize;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 20:03
 * @description :
 */
public interface Serializer {
    /**
     * 序列化
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化
     * @param bytes 序列化后的字节数组
     * @param clazz 类
     * @param <T>
     * @return 反序列化的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
