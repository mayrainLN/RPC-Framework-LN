package studio.lh.transport.netty.client;

import studio.lh.dto.RpcResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 22:07
 * @description : 存放客户端尚未得到响应的请求
 */
public class UnprocessedRequests {
    private static ConcurrentHashMap<String, CompletableFuture<RpcResponse>> unprocessedResponseFutures = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse> future) {
        unprocessedResponseFutures.put(requestId, future);
    }

    public void remove(String requestId) {
        unprocessedResponseFutures.remove(requestId);
    }

    /**
     * 返回结果后调用次方法
     * @param rpcResponse
     */
    public void complete(RpcResponse rpcResponse) {
        CompletableFuture<RpcResponse> future = unprocessedResponseFutures.remove(rpcResponse.getRequestId());
        if (null != future) {
            // 将response放入future
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
