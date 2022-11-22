package studio.lh;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:33
 * @description :
 */
public class ServiceProviderMain {
    public static void main(String[] args) {
        RpcServer rpcServer = new RpcServer();
        /**
         * 主线程进入到register后，就会在while ((socket = server.accept()) != null)中阻塞、循环。
         * 所以当前版本的代码，只能注册一个服务。
         */
        rpcServer.register(new HelloServiceImpl2(), 5656);

        // TODO 修改实现方式，通过map存放service解决只能注册一个service
        System.out.println("后面的不会执行");
        rpcServer.register(new HelloServiceImpl(), 5656);
    }
}
