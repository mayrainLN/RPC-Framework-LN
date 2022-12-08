package studio.lh.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.transport.socket.SocketRpcClient;
import studio.lh.util.RpcMessageChecker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:37
 * @description : 动态代理客户端的对象
 */
public class RpcClientProxy implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientProxy.class);

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
        RpcResponse rpcResponse = null;
        // 返回的其实是future
        CompletableFuture<RpcResponse> completableFuture = (CompletableFuture<RpcResponse>) rpcClient.sendRpcRequest(rpcRequest);
        try {
            // 阻塞直到handler向future中放入结果
            rpcResponse = completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("方法调用请求发送失败", e);
            return null;
        }
        RpcMessageChecker.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();
    }
}
