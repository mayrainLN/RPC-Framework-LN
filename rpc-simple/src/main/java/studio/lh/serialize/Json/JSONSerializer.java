package studio.lh.serialize.Json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.lh.dto.RpcRequest;
import studio.lh.exception.SerializeException;
import studio.lh.serialize.Serializer;
import studio.lh.serialize.SerializerCodeEnum;

import java.io.IOException;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/29 19:33
 * @description : JACKSON序列化
 */
public class JSONSerializer implements Serializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONSerializer.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(Object obj) {
        try {
            // 使用Jackson序列化为字节数组
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            LOGGER.error("序列化时有错误发生:", e);
            throw new SerializeException("序列化时有错误发生");
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        try {
            // 读出object对象
            Object obj = objectMapper.readValue(bytes, clazz);
            // 服务端收到
            if (obj instanceof RpcRequest) {
                obj = handleRequest(obj);
            }
            return obj;
        } catch (IOException e) {
            LOGGER.error("序列化时有错误发生:", e);
            throw new SerializeException("序列化时有错误发生");
        }
    }

    /*
        这里由于使用JSON序列化和反序列化Object数组(参数值数组)，无法保证反序列化后仍然为原参数的类型
        需要重新判断处理
     */
    private Object handleRequest(Object obj) throws IOException {
        RpcRequest rpcRequest = (RpcRequest) obj;
        for (int i = 0; i < rpcRequest.getParamTypes().length; i++) {
            // 获取当前request的参数信息
            Class<?> clazz = rpcRequest.getParamTypes()[i];
            // 检查当前的参数是否可以强转到类型列表里指定的类型
            // 不行，重新序列化一下
            if (!clazz.isAssignableFrom(rpcRequest.getParameters()[i].getClass())) {
                // 写入参数到字节数组
                byte[] bytes = objectMapper.writeValueAsBytes(rpcRequest.getParameters()[i]);
                // 重新按照类型列表里指定的类型序列化，相当于重新转换
                rpcRequest.getParameters()[i] = objectMapper.readValue(bytes, clazz);
            }
        }
        return rpcRequest;
    }

    @Override
    public int getCode() {
        return SerializerCodeEnum.valueOf("JSON").getCode();
    }
}