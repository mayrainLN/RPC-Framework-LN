package studio.lh;

import studio.lh.dto.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:37
 * @description :
 */
public class PpcClientProxy implements InvocationHandler {
    private String host;
    private Integer port;

    public PpcClientProxy(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, PpcClientProxy.this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                .parameters(method.getParameters())
                .build();
        RpcClient rpcClient = new RpcClient();
        return rpcClient;
    }
}
