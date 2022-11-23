package studio.lh;

import studio.lh.remoting.socket.RpcClientProxy;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:29
 * @description :
 */
public class ClientMain {
    public static void main(String[] args) {
        RpcClientProxy rpcClientProxy = new RpcClientProxy("localhost", 5656);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String res = helloService.hello(new Hello("我是消息", "我是描述"));
        System.out.println(res);
    }
}
