package studio.lh.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.factory.ThreadPoolFactory;
import studio.lh.util.NacosUtil;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 14:32
 * @description :
 */
public class ShutdownHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);

    public static void addClearAllHook() {
        LOGGER.info("启动服务端自动注销服务功能,关闭后将自动注销所有服务");
        /**
         * 当jvm关闭的时候，会执行系统中已经设置的所有通过方法addShutdownHook添加的钩子
         * 当系统执行完这些钩子后，jvm才会关闭。所以这些钩子可以在jvm关闭的时候进行内存清理、对象销毁等操作。
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 向nacos注销服务
            NacosUtil.clearRegistry();
            // 关闭本机所有线程池
            ThreadPoolFactory.shutDownAll();
        }));
    }

}
