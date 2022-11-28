package studio.lh.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/28 9:56
 * @description :
 */
public class NacosServiceRegistry implements ServiceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(NacosServiceRegistry.class);

    // 注册中心地址，先写死成本地吧
    private static final String SERVER_ADDR = "127.0.0.1:8848";
    private static final NamingService NAMING_SERVICE;

    static {
        try {
            // 注册服务
            NAMING_SERVICE = NamingFactory.createNamingService(SERVER_ADDR);
        } catch (NacosException e) {
            LOGGER.error("连接到Nacos时有错误发生: ", e);
            throw new RpcException(RpcErrorMessageEnum.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
        }
    }

    /**
     * 注册服务到注册中心
     * @param serviceName 注册的服务名称
     * @param inetSocketAddress 服务地址
     */
    @Override
    public void register(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            NAMING_SERVICE.registerInstance(serviceName, inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        } catch (NacosException e) {
            LOGGER.error("注册服务时有错误发生:", e);
            throw new RpcException(RpcErrorMessageEnum.REGISTER_SERVICE_FAILED);
        }
    }

    /**
     * 从nacos注册中心获取服务实例地址
     * @param serviceName 服务名称
     * @return InetSocketAddress 服务实例地址
     */
    @Override
    public InetSocketAddress lookupService(String serviceName) {
        try {
            // 获取服务实例列表
            List<Instance> instances = NAMING_SERVICE.getAllInstances(serviceName);
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
