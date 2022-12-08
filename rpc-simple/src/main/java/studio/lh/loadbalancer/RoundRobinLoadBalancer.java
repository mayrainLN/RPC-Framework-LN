package studio.lh.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Random;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/12/8 23:01
 * @description : 轮询
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private int index = 0;

    /**
     * @param instances 注册在Nacos的节点实例列表
     * @return 轮询选择出的节点
     */
    @Override
    public Instance select(List<Instance> instances) {
        if (index == instances.size()) {
            index = index % instances.size();
        }
        return instances.get(index++);
    }
}
