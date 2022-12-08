package studio.lh.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Random;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/12/8 23:00
 * @description : 随机
 */
public class RandomLoadBalancer implements LoadBalancer {
    /**
     * @param instances 注册在Nacos的节点实例列表
     * @return 随机选择出的节点
     */
    @Override
    public Instance select(List<Instance> instances) {
        return instances.get(new Random().nextInt(instances.size()));
    }
}
