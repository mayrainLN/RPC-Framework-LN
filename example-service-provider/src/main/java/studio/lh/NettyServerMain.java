package studio.lh;

import studio.lh.provider.ServiceProvider;
import studio.lh.provider.ServiceProviderImpl;
import studio.lh.transport.netty.server.NettyRpcServer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 16:26
 * @description :
 */
public class NettyServerMain {
    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        // 向注册中心注册本机地址
        NettyRpcServer nettyRpcServer = new NettyRpcServer("127.0.0.1", 5657);
        // 发布本机服务
        nettyRpcServer.publishService(helloService, HelloService.class);
        nettyRpcServer.start();
    }
}
