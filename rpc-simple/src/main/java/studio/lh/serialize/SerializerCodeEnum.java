package studio.lh.serialize;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 19:48
 * @description : 字节流中标识序列化和反序列化器
 */

@AllArgsConstructor
@Getter
public enum SerializerCodeEnum {
    JSON(0),
    Kryo(1);
    private final int code;
}
