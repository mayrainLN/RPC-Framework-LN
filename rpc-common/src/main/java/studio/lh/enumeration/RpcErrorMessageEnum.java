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
    REGISTER_SERVICE_FAILED("注册服务失败"),
    FAILED_TO_CONNECT_TO_SERVICE_REGISTRY("连接注册中心失败"),
    CLIENT_CONNECT_SERVER_FAILURE("客户端连接服务端失败"),
//    SERVICE_NOT_SURVIVE("客户端连接服务端失败"),
    SERIALIZER_NOT_FOUND("没有找到指定的序列化器"),
    SERVICE_CAN_NOT_FOUND("没有找到指定的服务"),
    SERVICE_NOT_IMPLEMENT_ANY_INTERFACE("注册的服务没有实现任何接口"),
    SERVICE_INVOCATION_FAILURE("服务调用失败"),
    REQUEST_NOT_MATCH_RESPONSE("响应与请求号不匹配");
    private final String message;
}
