package studio.lh;

import studio.lh.registry.DefaultServiceRegistry;
import studio.lh.registry.ServiceRegistry;
import studio.lh.transport.socket.SocketRpcServer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:33
 * @description :
 */
public class SocketServiceMain {
    public static void main(String[] args) {
        ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
        SocketRpcServer socketRpcServer = new SocketRpcServer(serviceRegistry);
        serviceRegistry.register(new HelloServiceImpl());

        socketRpcServer.start(5656);
        System.out.println("后面的依然不会执行,需要预先注册好所有注册中心");
    }
}
