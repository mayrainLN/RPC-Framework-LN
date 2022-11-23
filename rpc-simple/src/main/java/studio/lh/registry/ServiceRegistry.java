package studio.lh.registry;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/23 16:08
 * @description : 注册中心接口
 */
public interface ServiceRegistry {
    <T> void register(T service);

    Object getService(String serviceName);
}
