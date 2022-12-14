package studio.lh.transport;

import studio.lh.serialize.Serializer;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/28 11:15
 * @description :
 */
public interface RpcServer {

    void start();

    <T> void publishService(T service, Class<T> serviceClass);

}
