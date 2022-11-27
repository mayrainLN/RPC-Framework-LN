package studio.lh.dto;

import lombok.*;
import studio.lh.enumeration.RpcResponseCode;

import java.io.Serializable;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 17:53
 * @description :
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RpcResponse<T> implements Serializable {
    /**
     * 请求唯一标识
     */
    private String requestId;
    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = -1119096729226242009L;
    /**
     * 响应码
     */
    private Integer code;
    /**
     * 响应消息
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;

    /**
     * 快速生成成功响应消息
     * @param data
     * @param <T>
     * @return
     */
    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(RpcResponseCode.SUCCESS.getCode());
        response.setMessage(RpcResponseCode.SUCCESS.getMessage());
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    /**
     * 快速根据状态对象生成异常响应消息
     * @param rpcResponseCode
     * @param <T>
     * @return
     */
    public static <T> RpcResponse<T> fail(RpcResponseCode rpcResponseCode) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(rpcResponseCode.getCode());
        response.setMessage(rpcResponseCode.getMessage());
        return response;
    }
}
