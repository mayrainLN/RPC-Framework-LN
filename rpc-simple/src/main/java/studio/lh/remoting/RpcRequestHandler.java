package studio.lh.remoting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcResponseCode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 16:50
 * @description :
 */
public class RpcRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcRequestHandler.class);

    /**
     *
     * @param rpcRequest 请求DTO
     * @param service 实现类
     * @return
     */
    public Object handle(RpcRequest rpcRequest, Object service) {
        Object result = null;
        try {
            result = invokeTargetMethod(rpcRequest, service);
            LOGGER.info("service:{} successful invoke method:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("occur exception", e);
        }
        return result;
    }

    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // 根据方法名、参数列表类型，从实现类获取目标方法
        Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
        if (null == method) {
            return RpcResponse.fail(RpcResponseCode.NOT_FOUND_METHOD);
        }
        return method.invoke(service, rpcRequest.getParameters());
    }
}
