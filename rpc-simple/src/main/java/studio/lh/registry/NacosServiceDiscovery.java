package studio.lh.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;
import studio.lh.util.NacosUtil;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 13:14
 * @description :
 */
public class NacosServiceDiscovery implements ServiceDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(NacosServiceRegistry.class);

    /**
     * 从nacos注册中心获取服务实例地址
     * @param serviceName 服务名称
     * @return InetSocketAddress 服务实例地址
     */
    @Override
    public InetSocketAddress lookupService(String serviceName) {
        try {
            List<Instance> instances = NacosUtil.getAllInstance(serviceName);
            if (instances.size() < 1) {
                throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND, "暂无可用的服务实例");
            }
            /**
             * 后续可以在此添加负载均衡策略
             */
            Instance instance = instances.get(0);
            return new InetSocketAddress(instance.getIp(), instance.getPort());
        } catch (NacosException e) {
            LOGGER.error("获取服务时有错误发生:", e);
        }
        return null;
    }
}
