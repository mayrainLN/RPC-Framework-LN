package studio.lh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:37
 * @description : 工作线程
 */
public class ServerWorkerThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerWorkerThread.class);
    private Socket socket;
    private Object service;

    public ServerWorkerThread(Socket socket, Object service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            // service即为接口实现类的实例对象, 使用反射, 根据接口名和方法参数获得实现类实例的Method对象
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            // 反射调用实现类的方法, Object暂存结果
            Object result = method.invoke(service, rpcRequest.getParameters());
            // 向TCP缓冲区写入结果
            objectOutputStream.writeObject(result);
            // 发送RPC调用结果
            objectOutputStream.flush();

            // 防止Socket关闭过快，客户端还没有读取到结果Socket就已经关闭
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("occur exception:", e);
        }
    }
}
