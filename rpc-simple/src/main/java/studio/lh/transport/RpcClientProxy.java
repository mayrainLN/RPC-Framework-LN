package studio.lh.transport;

import studio.lh.dto.RpcRequest;
import studio.lh.transport.socket.SocketRpcClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:37
 * @description : 动态代理客户端的对象
 */
public class RpcClientProxy implements InvocationHandler {
     /**
     * 用于发送请求给服务端，对应socket和netty两种实现方式
     */
    private RpcClient rpcClient;

    public RpcClientProxy(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
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
                // 生成请求ID
                .requestId(UUID.randomUUID().toString())
                .build();
        // 代理过程中获得一个rpcClient的实例, 调用实例的sendRpcRequest方法

        return rpcClient.sendRpcRequest(rpcRequest);
    }
}
