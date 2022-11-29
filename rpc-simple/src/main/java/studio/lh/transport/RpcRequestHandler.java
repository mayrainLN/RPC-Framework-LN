package studio.lh.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcResponseCode;
import studio.lh.provider.ServiceProvider;
import studio.lh.provider.ServiceProviderImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 16:50
 * @description : 真正发起服务调用的对象
 */
public class RpcRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcRequestHandler.class);

    // 当前实例所提供的服务Map
    private static final ServiceProvider SERVICE_PROVIDER;

    static {
        SERVICE_PROVIDER = new ServiceProviderImpl();
    }

    /**
     * 请求已经发到本服务上了，本机就是被"发现"的服务，所以只管调用发过来的请求就好了
     * @param rpcRequest 请求DTO
     * @return 调用结果
     */
    public Object handle(RpcRequest rpcRequest) {
        // 从本地的serviceProvider获取服务实现的实例
        Object service = SERVICE_PROVIDER.getService(rpcRequest.getInterfaceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result = null;
        // 根据方法名、参数列表类型，从实现类获取目标方法
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            LOGGER.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return RpcResponse.fail(RpcResponseCode.NOT_FOUND_METHOD);
        }
        return result;
    }
}
