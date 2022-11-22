package monolithic;

import studio.lh.RpcServer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 20:37
 * @description :
 */
public class ServerTest {
    public static void main(String[] args) {
        RpcServer rpcServer = new RpcServer();
        rpcServer.register(new ServiceImplTest(),5656);
    }
}
