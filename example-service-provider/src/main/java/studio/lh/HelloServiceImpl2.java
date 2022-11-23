package studio.lh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:55
 * @description :
 */
public class HelloServiceImpl2 implements HelloService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloServiceImpl2.class);
    @Override
    public String hello(Hello hello) {
        LOGGER.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription() + hello.getMessage();
        LOGGER.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
