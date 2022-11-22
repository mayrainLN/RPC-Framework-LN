package studio.lh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcResponseCode;

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
public class RequestHandlerThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerThread.class);
    private Socket socket;
    private Object service;

    public RequestHandlerThread(Socket socket, Object service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            // service即为接口实现类的实例对象, 使用反射, 根据接口名和方法参数获得实现类实例的Method对象
            Object result = invokeTargetMethod(rpcRequest);
            objectOutputStream.writeObject(RpcResponse.success(result));
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

    private Object invokeTargetMethod(RpcRequest rpcRequest) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {
        //初始化指定的类 动态加载 如InterfaceA 类
        Class<?> cls = Class.forName(rpcRequest.getInterfaceName());
        LOGGER.info("客户端调用:{}接口{}方法", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        // 接口cls是否是实现类service的父类
        if (!cls.isAssignableFrom(service.getClass())) {
            return RpcResponse.fail(RpcResponseCode.NOT_FOUND_CLASS);
        }
        // 反射获取目标方法
        Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
        if (null == method) {
            return RpcResponse.fail(RpcResponseCode.NOT_FOUND_METHOD);
        }
        // 返回方法调用结果
        return method.invoke(service, rpcRequest.getParameters());
    }
}
