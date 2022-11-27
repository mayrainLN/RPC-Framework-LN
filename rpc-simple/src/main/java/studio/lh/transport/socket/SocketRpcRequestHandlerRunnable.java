package studio.lh.transport.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.registry.DefaultServiceRegistry;
import studio.lh.registry.ServiceRegistry;
import studio.lh.transport.RpcRequestHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 16:55
 * @description :
 */
public class SocketRpcRequestHandlerRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketRpcRequestHandlerRunnable.class);
    private Socket socket;
    // 处理者 由他发起真正的调用
    private static RpcRequestHandler rpcRequestHandler;
    private static ServiceRegistry serviceRegistry;

    static {
        rpcRequestHandler = new RpcRequestHandler();
        serviceRegistry = new DefaultServiceRegistry();
    }

    public SocketRpcRequestHandlerRunnable(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            String interfaceName = rpcRequest.getInterfaceName();
            // 获取实现类
            Object service = serviceRegistry.getService(interfaceName);
            // 由Handler对象执行调用
            Object result = rpcRequestHandler.handle(rpcRequest, service);
            objectOutputStream.writeObject(RpcResponse.success(result, rpcRequest.getRequestId()));
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("occur exception:", e);
        }
    }
}
