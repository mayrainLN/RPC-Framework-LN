package studio.lh.remoting.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.registry.ServiceRegistry;
import studio.lh.remoting.RpcRequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:36
 * @description :
 */
public class RpcServer {

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAXIMUM_POOL_SIZE_SIZE = 100;
    private static final int KEEP_ALIVE_TIME = 1;
    private static final int BLOCKING_QUEUE_CAPACITY = 100;
    // 本地注册中心
    private final ServiceRegistry serviceRegistry;
    // 线程池用于执行方法
    private ExecutorService threadPool;
    // 线程中的方法执行者
    private RpcRequestHandler rpcRequestHandler = new RpcRequestHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    /**
     * 每个Server实例对应单独的线程池、Handler
     * 只需传入注册中心即可，其他的成员都重新构造
     * @param serviceRegistry
     */
    public RpcServer(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPACITY);
        // 线程工厂
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        // 每个Server实例对应一个线程池
        this.threadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE_SIZE, KEEP_ALIVE_TIME, TimeUnit.MINUTES, workQueue, threadFactory);
    }

    // 一个Server实例对应一个线程池
    public void start(int port) {

        try (ServerSocket server = new ServerSocket(port);) {
            LOGGER.info("server starts...");
            Socket socket;
            while ((socket = server.accept()) != null) {
                LOGGER.info("client connected");
                threadPool.execute(new RpcRequestHandlerRunnable(socket, rpcRequestHandler, serviceRegistry));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            LOGGER.error("occur IOException:", e);
        }
    }

}

