package studio.lh.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 13:43
 * @description : 基于HashMap的泛型工厂
 */
public class SingletonFactory {

    private static final Map<Class, Object> OBJECT_MAP = new HashMap<>();

    public static <T> T getInstance(Class<T> clazz) {
        Object instance = OBJECT_MAP.get(clazz);
        /**
         * 加锁保证线程安全
         * 还有优化的余地：doubleCheck
         */
        synchronized (clazz) {
            if (instance == null) {
                try {
                    instance = clazz.newInstance();
                    OBJECT_MAP.put(clazz, instance);
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        // 强转类型
        return clazz.cast(instance);
    }

}
