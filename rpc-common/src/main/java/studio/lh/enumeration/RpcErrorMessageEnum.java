package studio.lh.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 18:06
 * @description :错误类型 [枚举]
 */
@AllArgsConstructor
@Getter
@ToString
public enum RpcErrorMessageEnum implements Serializable {
    SERVICE_INVOCATION_FAILURE("服务调用失败"),
    SERVICE_CAN_NOT_BE_NULL("注册的服务不能为空");
    private final String message;
}