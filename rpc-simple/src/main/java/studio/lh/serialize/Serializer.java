package studio.lh.serialize;

import studio.lh.serialize.Json.JSONSerializer;
import studio.lh.serialize.kryo.KryoSerializer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 20:03
 * @description :
 */
public interface Serializer {

    Integer KRYO_SERIALIZER = 0;
    Integer JSON_SERIALIZER = 1;

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
     * @return 反序列化的对象
     */
    Object deserialize(byte[] bytes, Class<?> clazz);

    static Serializer getSerializer(int code) {
        switch (code) {
            case 0:
                return new KryoSerializer();
            case 1:
                return new JSONSerializer();
            default:
                return null;
        }
    }

    int getCode();
}
