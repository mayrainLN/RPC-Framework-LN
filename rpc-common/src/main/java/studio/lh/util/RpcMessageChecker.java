package studio.lh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.enumeration.RpcResponseCode;
import studio.lh.exception.RpcException;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 17:46
 * @description :
 */
public final class RpcMessageChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcMessageChecker.class);
    public static final String INTERFACE_NAME = "interfaceName";

    private RpcMessageChecker() {
    }

    /**
     * 检查请求和响应是否匹配
     * @param rpcResponse
     * @param rpcRequest
     */
    public static void check(RpcResponse rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            LOGGER.error("调用服务失败,serviceName:{}", rpcRequest.getInterfaceName());
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCode.SUCCESS.getCode())) {
            LOGGER.error("调用服务失败,serviceName:{},RpcResponse:{}", rpcRequest.getInterfaceName(), rpcResponse);
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
