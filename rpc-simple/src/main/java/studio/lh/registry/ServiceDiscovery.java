package studio.lh.registry;

import java.net.InetSocketAddress;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 13:12
 * @description :
 */
public interface ServiceDiscovery {

    /**
     * 根据服务名称查找服务实体
     *
     * @param serviceName 服务名称
     * @return 服务实体
     */
    InetSocketAddress lookupService(String serviceName);

}
