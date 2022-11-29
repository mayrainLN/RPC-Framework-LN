package studio.lh.util;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 12:51
 * @description : 与Nacos通信的工具类
 */
public class NacosUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosUtil.class);

    /**
     * 先写死注册中心的地址
     */
    private static final String SERVER_ADDR = "127.0.0.1:8848";

    private static final NamingService NACOS_NAMING_SERVICE = getNacosNamingService();

    /**
     * 存储本机已经注册的服务名称，用于后续注销服务
     */
    private static final Set<String> REGISTERED_SERVICES_NAMES = new HashSet<>();

    /**
     * 本机地址
     */
    private static InetSocketAddress address;

    public static NamingService getNacosNamingService() {
        try {
            return NamingFactory.createNamingService(SERVER_ADDR);
        } catch (NacosException e) {
            LOGGER.error("连接到Nacos时有错误发生: ", e);
            throw new RpcException(RpcErrorMessageEnum.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
        }
    }

    public static void registerService(String serviceName, InetSocketAddress address) throws NacosException {
        NACOS_NAMING_SERVICE.registerInstance(serviceName, address.getHostName(), address.getPort());
        // 储存本地地址
        NacosUtil.address = address;
        // 存储已经注册的服务
        REGISTERED_SERVICES_NAMES.add(serviceName);
    }

    public static List<Instance> getAllInstance(String serviceName) throws NacosException {
        return NACOS_NAMING_SERVICE.getAllInstances(serviceName);
    }

    /**
     * 注销本机所有服务：遍历本机已经注册的所有服务，向Nacos发出信息
     */
    public static void clearRegistry() {
        //
        if (!REGISTERED_SERVICES_NAMES.isEmpty() && address != null) {
            String host = address.getHostName();
            int port = address.getPort();
            Iterator<String> iterator = REGISTERED_SERVICES_NAMES.iterator();
            while (iterator.hasNext()) {
                String serviceName = iterator.next();
                try {
                    // 向nacos注销本机的服务
                    NACOS_NAMING_SERVICE.deregisterInstance(serviceName, host, port);
                    LOGGER.info("已注销服务: {} @ {}:{}", serviceName, host, port);
                } catch (NacosException e) {
                    LOGGER.error("注销服务 {} 失败", serviceName, e);
                }
            }
        }
    }
}
