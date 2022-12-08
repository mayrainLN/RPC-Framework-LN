package studio.lh;

import studio.lh.serialize.Serializer;
import studio.lh.transport.RpcClient;
import studio.lh.transport.RpcClientProxy;
import studio.lh.transport.netty.client.NettyRpcClient;

import java.util.concurrent.CompletableFuture;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 16:07
 * @description :
 */
public class NettyClientMain {
    public static void main(String[] args) {
        RpcClient client = new NettyRpcClient(Serializer.JSON_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        // 获取代理的service实例对象
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        Hello object = new Hello("1111", "1111");
        String res = helloService.hello(object);
        System.out.println(res);
    }
}
