package studio.lh.transport;

import studio.lh.dto.RpcRequest;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 20:53
 * @description :
 */
public interface RpcClient {
    Object sendRpcRequest(RpcRequest rpcRequest);
}
