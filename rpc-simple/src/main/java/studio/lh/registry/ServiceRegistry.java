package studio.lh.registry;

import java.net.InetSocketAddress;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 16:08
 * @description : 注册中心接口
 */
public interface ServiceRegistry {
    /**
     * 注册服务
     * @param serviceName 注册的服务名称
     * @param inetSocketAddress 服务地址
     */
    void register(String serviceName, InetSocketAddress inetSocketAddress);
}
