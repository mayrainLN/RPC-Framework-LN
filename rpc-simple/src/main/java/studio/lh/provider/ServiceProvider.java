package studio.lh.provider;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/28 10:08
 * @description : 存储服务实例的信息(本地)
 */
public interface ServiceProvider {
    <T> void addService(T service);
    Object getService(String serviceName);
}
