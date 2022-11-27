package studio.lh.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.enumeration.RpcErrorMessageEnum;
import studio.lh.exception.RpcException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 16:10
 * @description : 本地注册中心
 */
public class DefaultServiceRegistry implements ServiceRegistry {
    public static final Logger LOGGER = LoggerFactory.getLogger(DefaultServiceRegistry.class);

    /**
     * key: 接口类名 （完整类名）
     * value: 实现类
     * TODO 当前一个接口只能对应一个实现类
     */
    private static final Map<String, Object> SERVICE_MAP = new ConcurrentHashMap<>();

    private static final Set<String> REGISTERED_SERVICE = ConcurrentHashMap.newKeySet();

    /**
     * 加锁,保证注册的线程安全
     * 注册某个实现类。其实现的接口是反射获取的,无需传入参数
     * @param service 实现类
     * @param <T> 泛型
     */
    @Override
    public synchronized  <T> void register(T service) {
        // 获取规范类名
        String serviceName = service.getClass().getCanonicalName();
        if (REGISTERED_SERVICE.contains(serviceName)) {
            return;
        }
        REGISTERED_SERVICE.add(serviceName);
        // 获取该实现类实现的所有接口 的Class对象
        Class[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length == 0) {
            // 注册的服务没有实现任何接口
            throw new RpcException(RpcErrorMessageEnum.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
        }
        for (Class i : interfaces) {
            // 某个被实现的接口 ： 当前实现类
            SERVICE_MAP.put(i.getCanonicalName(), service);
        }
        LOGGER.info("Add service: {} and interfaces:{}", serviceName, service.getClass().getInterfaces());
    }


    /**
     * 线程安全
     * 根据接口获取实现类
     * @param serviceName 接口名
     * @return 实现类
     */
    @Override
    public synchronized Object getService(String serviceName) {
        Object service = SERVICE_MAP.get(serviceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_FOUND);
        }
        return service;
    }
}
