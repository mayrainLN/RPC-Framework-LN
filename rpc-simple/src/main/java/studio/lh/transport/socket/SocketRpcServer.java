package studio.lh.transport.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.provider.ServiceProvider;
import studio.lh.provider.ServiceProviderImpl;
import studio.lh.registry.NacosServiceRegistry;
import studio.lh.registry.ServiceRegistry;
import studio.lh.serialize.Serializer;
import studio.lh.transport.RpcServer;
import studio.lh.factory.ThreadPoolFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:36
 * @description :
 */
public class SocketRpcServer implements RpcServer {
    private String host;
    private int port;
    // 注册中心
    private final ServiceRegistry serviceRegistry;
    // 服务本地Map
    private final ServiceProvider serviceProvider;
    // 线程池用于执行方法
    private ExecutorService threadPool;
    // 线程中的方法执行者
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketRpcServer.class);

    /**
     * 启动本机实例时要指明ip和端口
     *
     * @param host
     * @param port
     */
    public SocketRpcServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.serviceRegistry = new NacosServiceRegistry();
        this.serviceProvider = new ServiceProviderImpl();
        threadPool = ThreadPoolFactory.createDefaultThreadPool("socket-rpc-server");
    }

    // 一个Server实例对应一个线程池
    public <T> void publishService(T service, Class<T> serviceClass) {
        serviceProvider.addService(service, serviceClass);
        serviceRegistry.register(serviceClass.getCanonicalName(), new InetSocketAddress(host, port));
    }

    @Override
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("服务器启动……");
            Socket socket;
            while ((socket = serverSocket.accept()) != null) {
                LOGGER.info("消费者连接: {}:{}", socket.getInetAddress(), socket.getPort());
                // 连接建立后，交由给线程池去执行
                threadPool.execute(new SocketRpcRequestHandlerRunnable(socket));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            LOGGER.error("服务器启动时有错误发生:", e);
        }
    }
}

