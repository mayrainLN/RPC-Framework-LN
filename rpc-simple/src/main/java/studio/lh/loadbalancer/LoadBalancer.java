package studio.lh.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/12/8 22:58
 * @description : 负载均衡器接口
 */
public interface LoadBalancer {

    /**
     *
     * @param instances 注册在Nacos的节点实例列表
     * @return 选择出的节点
     */
    Instance select(List<Instance> instances);

}
