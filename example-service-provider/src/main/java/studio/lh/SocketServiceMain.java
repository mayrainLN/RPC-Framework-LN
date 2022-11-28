package studio.lh;

import studio.lh.transport.socket.SocketRpcServer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:33
 * @description :
 */
public class SocketServiceMain {
    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        SocketRpcServer socketRpcServer = new SocketRpcServer("localhost", 5656);
        socketRpcServer.publishService(helloService, HelloService.class);
        socketRpcServer.start();
        System.out.println("后面的依然不会执行,需要预先注册好所有注册中心");
    }
}
