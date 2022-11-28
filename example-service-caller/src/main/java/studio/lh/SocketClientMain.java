package studio.lh;

import studio.lh.transport.RpcClient;
import studio.lh.transport.RpcClientProxy;
import studio.lh.transport.socket.SocketRpcClient;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:29
 * @description :
 */
public class SocketClientMain {
    public static void main(String[] args) {
        // 其中的注册中心地址已经写死了
        RpcClient rpcClient = new SocketRpcClient();
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String res = helloService.hello(new Hello("我是消息", "我是描述"));
        System.out.println(res);
    }
}
