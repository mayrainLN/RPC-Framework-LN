package studio.lh.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.Hello;
import studio.lh.HelloService;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:17
 * @description :
 */
public class HelloServiceImpl implements HelloService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloServiceImpl.class);
    @Override
    public String hello(Hello hello) {
        LOGGER.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        LOGGER.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
