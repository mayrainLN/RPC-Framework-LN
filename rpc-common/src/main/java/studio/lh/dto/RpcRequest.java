package studio.lh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 19:42
 * @description : Rpc请求的
 */
@Data
@Builder
@AllArgsConstructor
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = -7287071775732594012L;
    /**
     * 接口名
     */
    private String interfaceName;
    /**
     * 方法名
     */
    private String methodName;
    /**
     * 参数值 列表
     */
    private Object[] parameters;
    /**
     * 参数类型 列表
     */
    private Class<?>[] paramTypes;
}
