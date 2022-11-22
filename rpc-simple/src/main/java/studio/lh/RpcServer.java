package studio.lh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;

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
    // 线程池用于执行方法
    private ExecutorService threadPool;
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    public RpcServer() {
        // 线程池参数
        int corePoolSize = 10;
        int maximumPoolSizeSize = 100;
        long keepAliveTime = 1;

        // 任务队列，用于保存等待执行的任务的阻塞队列。
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        this.threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSizeSize, keepAliveTime, TimeUnit.MINUTES, workQueue, threadFactory);
    }

    /**
     * 服务端主动注册服务，创建Socket在指定的端口监听调用服务的调用请求
     * 具体服务的执行，由交给线程池去完成。Socket只用于监听请求
     * TODO 引入Spring,扫描自定义注解然后自动注册服务
     */
    public void register(Object service, int port) {
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_NULL);
        }

        /**
         * 如果主线程走到这里，主线程就不糊执行了，阻塞到这里一直监听请求。所以我们最终只能注册一个服务
         */
        try (ServerSocket server = new ServerSocket(port);) {
            LOGGER.info("server starts...");
            Socket socket;
            while ((socket = server.accept()) != null) {
                LOGGER.info("client connected");
                // 向线程池中提交任务(创建一个,Runnable的实现类的实例,规定了要做的事情, 传入到线程池中)
                threadPool.execute(new RequestHandlerThread(socket, service));
            }
        } catch (IOException e) {
            LOGGER.error("occur IOException:", e);
        }
    }
}

