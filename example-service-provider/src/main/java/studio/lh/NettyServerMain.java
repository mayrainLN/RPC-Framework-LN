package studio.lh;

import studio.lh.registry.DefaultServiceRegistry;
import studio.lh.transport.netty.server.NettyRpcServer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 16:26
 * @description :
 */
public class NettyServerMain {
    public static void main(String[] args) {
        HelloServiceImpl helloService = new HelloServiceImpl();
        DefaultServiceRegistry defaultServiceRegistry = new DefaultServiceRegistry();
        // 手动注册
        defaultServiceRegistry.register(helloService);
        NettyRpcServer nettyRpcServer = new NettyRpcServer(5656);
        nettyRpcServer.run();
    }
}
