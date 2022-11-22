package studio.lh.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 18:07
 * @description : 响应状态码
 */
@AllArgsConstructor
@Getter
@ToString
public enum  RpcResponseCode implements Serializable {
    // 源数据
    SUCCESS(200, "调用方法成功"),
    FAIL(500, "调用方法失败"),
    NOT_FOUND_METHOD(500, "未找到指定方法"),
    NOT_FOUND_CLASS(500, "未找到指定类");
    // 响应实例的状态码字段
    private final int code;
    // 响应实例的状态信息字段
    private final String message;
}
