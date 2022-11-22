package monolithic;

import studio.lh.RpcClient;
import studio.lh.dto.RpcRequest;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 20:37
 * @description :
 */
public class ClientTest {
    public static void main(String[] args) {
        RpcClient rpcClient= new RpcClient();
        String serviceName = "ServiceTest";
        String methodName = "reverseEcho";
        Object[] params = new Object[]{"hello world"};
        Class[] clazz = new Class[]{String.class};
        RpcRequest rpcRequest = new RpcRequest(serviceName, methodName, params, clazz);

        String  response =(String) rpcClient.sendRpcRequest(
                new RpcRequest(serviceName, methodName, params, clazz),
                "localhost",
                5656
        );
        System.out.println("客户端收到结果："+response);
    }
}
