package studio.lh.remoting.socket;

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
public class RpcClientProxy implements InvocationHandler {
    private String host;
    private Integer port;

    public RpcClientProxy(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                //BUG 傻逼 不是method.getParameters()
                .parameters(args)
                .build();
        // 代理过程中获得一个rpcClient的实例, 调用实例的sendRpcRequest方法
        RpcClient rpcClient = new RpcClient();
        return rpcClient.sendRpcRequest(rpcRequest, host, port);
    }
}
