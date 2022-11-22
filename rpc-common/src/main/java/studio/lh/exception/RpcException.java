package studio.lh.exception;

import studio.lh.enumeration.RpcErrorMessageEnum;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 18:11
 * @description : 公共异常
 */
public class RpcException extends RuntimeException {
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }
}
