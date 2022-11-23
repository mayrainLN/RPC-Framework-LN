package studio.lh.remoting.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.dto.RpcResponse;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.enumeration.RpcResponseCode;
import studio.lh.exception.RpcException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:36
 * @description :
 */
public class RpcClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    /**
     * 发起RPC调用
     * @param rpcRequest 服务请求DTO
     * @param host 服务端地址
     * @param port 服务端端口
     * @return 响应数据(不包含状态码等)
     */
    public Object sendRpcRequest(RpcRequest rpcRequest, String host, Integer port) {
        try (Socket socket = new Socket(host, port)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // 向Socket中发送请求
            objectOutputStream.writeObject(rpcRequest);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 返回远程调用结果
            RpcResponse rpcResponse = (RpcResponse) objectInputStream.readObject();
            if (null == rpcResponse) {
                LOGGER.error("RPC调用失败,serviceName:{}", rpcRequest.getInterfaceName());
                throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interfaceName:" + rpcRequest.getInterfaceName());
            }
            // 状态码为空或者状态码不为成功状态码
            if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCode.SUCCESS.getCode())) {
                LOGGER.error("RPC调用失败,serviceName:{},RpcResponse:{}", rpcRequest.getInterfaceName(), rpcResponse);
                throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interfaceName:" + rpcRequest.getInterfaceName());
            }
            return rpcResponse.getData();

        } catch (IOException | ClassNotFoundException e) {
            throw new RpcException("RPC调用失败:", e);
        }
    }
}
