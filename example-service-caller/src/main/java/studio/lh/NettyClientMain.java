package studio.lh;

import studio.lh.transport.RpcClient;
import studio.lh.transport.RpcClientProxy;
import studio.lh.transport.netty.client.NettyRpcClient;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 16:07
 * @description :
 */
public class NettyClientMain {
    public static void main(String[] args) {
        RpcClient rpcClient = new NettyRpcClient("127.0.0.1", 5656);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello1 = helloService.hello(new Hello("netty信息555", "netty描述555"));
        String hello2 = helloService.hello(new Hello("netty信息666", "netty描述666"));
        System.out.println(hello1);
        System.out.println(hello2);
//        assert "Hello description is netty描述444".equals(hello);
    }
}
