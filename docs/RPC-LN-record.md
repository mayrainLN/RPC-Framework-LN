# [v0.9]  BIO | 动态代理  | JDK序列化

**实现BIO传输，客户端动态代理，服务端调用返回结果**

## 初始化项目结构

![image-20221121213809902](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211212138444.png)

## 使用效果概览

服务端**主动**注册服务(实现类)

![image-20221121213358639](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211212201179.png)

客户端(**仅依赖于接口，不依赖于实现**)  调用服务： 返回echo结果 `dlrow olleh`

![image-20221121213124931](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211212201270.png)

## 框架实现

### 客户端框架

服务端和客户端都要依赖于`Common`模块，其中定义DTO。 

客户端中，对接口`ServiceTest`的方法调用，不需要new出来其实现类`ServiceTestImpl`，而是调用`socketRpcClient.sendRpcRequest

```java
public class RpcClient {
    public static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    public Object sendRpcRequest(RpcRequest rpcRequest,String host,Integer port){
        try (Socket socket = new Socket(host,port)){
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // 向Socket中发送请求
            objectOutputStream.writeObject(rpcRequest);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 返回远程调用结果
            return objectInputStream.readObject();
        }catch (IOException | ClassNotFoundException e){
            logger.error("occur exception:",e);
        }
        return null;
    }
}
```

在`RpcClientProxy`中，动态代理进行BIO数据传输。

测试版本的代码其实并没有走动态代理，而是直接调用的RpcClient的实例的`sendRpcRequest`方法

```java
public class PpcClientProxy implements InvocationHandler {
    private String host;
    private Integer port;

    public PpcClientProxy(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public <T> T getProxy(Class<T> clazz){
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, PpcClientProxy.this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                .parameters(method.getParameters())
                .build();
        RpcClient socketRpcClient = new RpcClient();
        return socketRpcClient;
    }
}

```

### 服务端框架

服务端主程序需要主动注册接口(实现类)。

注册时，为实现类创建一个Socket，专用于监听对该接口的调用监听。监听到请求后，将socket、service封装成`ServerWorkerThread`，放入到线程池中等待执行。

```java
public class RpcServer {
    // 线程池用于执行方法
    private ExecutorService threadPool;
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    public RpcServer() {
        // 线程池参数
        int corePoolSize = 10;
        int maximumPoolSizeSize = 100;
        long keepAliveTime = 1;

        // 任务队列，用于保存等待执行的任务的阻塞队列。
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        this.threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSizeSize, keepAliveTime, TimeUnit.MINUTES, workQueue, threadFactory);
    }

    /**
     * 服务端主动注册服务，创建Socket在指定的端口监听调用该服务的调用请求
     * 具体服务的执行，由交给线程池去完成。Socket只用于监听请求
     * TODO 引入Spring,扫描自定义注解然后自动注册服务
     */
    public void register(Object service, int port) {
        try (ServerSocket server = new ServerSocket(port);) {
            logger.info("server starts...");
            Socket socket;
            while ((socket = server.accept()) != null) {
                logger.info("client connected");
                // 向线程池中提交任务(创建一个,Runnable的实现类的实例,规定了要做的事情, 传入到线程池中)
                threadPool.execute(new ServerWorkerThread(socket, service));
            }
        } catch (IOException e) {
            logger.error("occur IOException:", e);
        }
    }
}
```

为了进行传入，**DTO需要实现序列化接口。**

```java
public class ServerWorkerThread implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(ServerWorkerThread.class);
    private Socket socket;
    private Object service;

    public ServerWorkerThread(Socket socket, Object service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            // service即为接口实现类的实例对象, 使用反射, 根据接口名和方法参数获得实现类实例的Method对象
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            // 反射调用实现类的方法, Object暂存结果
            Object result = method.invoke(service, rpcRequest.getParameters());
            // 向TCP缓冲区写入结果
            objectOutputStream.writeObject(result);
            // 发送RPC调用结果
            objectOutputStream.flush();

            // 防止Socket关闭过快，客户端还没有读取到结果Socket就已经关闭
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error("occur exception:", e);
        }
    }
}
```

## TODO

1. 没有注册中心、负载均衡的组件，基本上只是个玩具。
2. 服务端主线程被阻塞，只能监听一个服务。
3. JDK原生序列化性能很差，需要引入第三方库。
4. 为了方便测试，没有继续划分模块。所以客户端、服务端、实现类的代码耦合在一起，没有划分子项目，等到后面可以当成基本的框架用再进行划分也不迟。



# [v0.92] 自定义异常 | 状态码信息

## 效果预览

**服务调用者**

![image-20221122222856449](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211222302300.png)

**服务提供者**

![image-20221122223017431](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211222301099.png)



## 新增内容

- 服务接口、服务调用者、服务实现者 各自演化为独立项目, 依赖于RPC框架。贴合实际。
- 将接口定义独立出来。而接口实现放在服务端。这正是RPC框架存在的意义。
- 服务端可以提供不同版本的接口实现
- 完善服务端的异常处理
- 引入状态码、状态信息,完善返回的异常信息
- 修复服务端反射调用的错误参数传递。



## 实现

**完善、修复代理类**

```java
public class RequestHandlerThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerThread.class);
    private Socket socket;
    private Object service;

    public RequestHandlerThread(Socket socket, Object service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            // service即为接口实现类的实例对象, 使用反射, 根据接口名和方法参数获得实现类实例的Method对象
            Object result = invokeTargetMethod(rpcRequest);
            objectOutputStream.writeObject(RpcResponse.success(result));
            // 发送RPC调用结果
            objectOutputStream.flush();

            // 防止Socket关闭过快，客户端还没有读取到结果Socket就已经关闭
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("occur exception:", e);
        }
    }

    private Object invokeTargetMethod(RpcRequest rpcRequest) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {
        //初始化指定的类 动态加载 如InterfaceA 类
        Class<?> cls = Class.forName(rpcRequest.getInterfaceName());
        LOGGER.info("客户端调用:{}接口{}方法", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        // 接口cls是否是实现类service的父类
        if (!cls.isAssignableFrom(service.getClass())) {
            return RpcResponse.fail(RpcResponseCode.NOT_FOUND_CLASS);
        }
        // 反射获取目标方法
        Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
        if (null == method) {
            return RpcResponse.fail(RpcResponseCode.NOT_FOUND_METHOD);
        }
        // 返回方法调用结果
        return method.invoke(service, rpcRequest.getParameters());
    }
}
```

错误信息枚举

```java
@AllArgsConstructor
@Getter
@ToString
public enum RpcErrorMessageEnum implements Serializable {
    SERVICE_INVOCATION_FAILURE("服务调用失败"),
    SERVICE_CAN_NOT_BE_NULL("注册的服务不能为空");
    private final String message;
}
```

状态码枚举

```java
public enum  RpcResponseCode implements Serializable {
    // 源数据
    SUCCESS(200, "调用方法成功"),
    FAIL(500, "调用方法失败"),
    NOT_FOUND_METHOD(500, "未找到指定方法"),
    NOT_FOUND_CLASS(500, "未找到指定类");
    // 响应实例的状态码字段
    private final int code;
    // 响应实例的状态信息字段
    private final String message;

}
```



客户端和服务端项目省略。需要注意的是，他们**都需要依赖于service-api**【服务接口定义，可以说是依赖倒转】、**rpc-simple**【框架本体】。



具体的错误处理内容，分散在项目的各个部分，不好展示，详见当前版本的github
 https://github.com/mayrainLN/RPC-Framework-LN/commits/master`df864829bc54611fbc657ab6db4a4aca0d15afc6` 与 `7b3e63e3f375f09cab1c5b337e5087750c3c68e7` 两个提交



## TODO

在服务端中，主线程注册代码，会进入如下代码。所以主线程会阻塞、循环。因此我们当前的版本，一个服务端实例只能注册一个服务接口。

```java
try (ServerSocket server = new ServerSocket(port);)
```

下一版本解决此问题



# [v1.0] 本地注册中心

## 效果预览

客户端不变，在此展示服务端

![image-20221123175624635](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211231957605.png)



## 新增内容

- 引入本地注册中心，在服务启动监听前，预先注册好所有服务，就可以让Socket监听多个接口的调用请求。

## 实现

1. 抽象出Handler对象真正执行方法，不再和Runnale耦合

   ```java
   public class RpcRequestHandler {
       private static final Logger LOGGER = LoggerFactory.getLogger(RpcRequestHandler.class);
   
       /**
        *
        * @param rpcRequest 请求DTO
        * @param service 实现类
        * @return
        */
       public Object handle(RpcRequest rpcRequest, Object service) {
           Object result = null;
           try {
               result = invokeTargetMethod(rpcRequest, service);
               LOGGER.info("service:{} successful invoke method:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
           } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
               LOGGER.error("occur exception", e);
           }
           return result;
       }
   
       private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
           // 根据方法名、参数列表类型，从实现类获取目标方法
           Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
           if (null == method) {
               return RpcResponse.fail(RpcResponseCode.NOT_FOUND_METHOD);
           }
           return method.invoke(service, rpcRequest.getParameters());
       }
   }
   ```

2. 由Runable代表一个需要执行的调用请求。所以需要传入socket和具体的handler对象以及注册中心

   ```java
   public class RpcRequestHandlerRunnable implements Runnable {
       private static final Logger LOGGER = LoggerFactory.getLogger(RpcRequestHandlerRunnable.class);
       private Socket socket;
       // 处理者 由他发起真正的调用
       private RpcRequestHandler rpcRequestHandler;
       private ServiceRegistry serviceRegistry;
   
       public RpcRequestHandlerRunnable(Socket socket, RpcRequestHandler rpcRequestHandler, ServiceRegistry serviceRegistry) {
           this.socket = socket;
           this.rpcRequestHandler = rpcRequestHandler;
           this.serviceRegistry = serviceRegistry;
       }
   
       @Override
       public void run() {
           try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
               RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
               String interfaceName = rpcRequest.getInterfaceName();
               // 获取实现类
               Object service = serviceRegistry.getService(interfaceName);
               // 由Handler对象执行调用
               Object result = rpcRequestHandler.handle(rpcRequest, service);
               objectOutputStream.writeObject(RpcResponse.success(result));
               objectOutputStream.flush();
           } catch (IOException | ClassNotFoundException e) {
               LOGGER.error("occur exception:", e);
           }
       }
   }
   ```

3. 注册中心由`ConcurrentHashMap`承担储存功能，注册和获取服务的接口都加方法锁，保证线程安全。

   ```java
   public class DefaultServiceRegistry implements ServiceRegistry {
       public static final Logger LOGGER = LoggerFactory.getLogger(DefaultServiceRegistry.class);
   
       /**
        * key: 接口类名 （完整类名）
        * value: 实现类
        * TODO 当前一个接口只能对应一个实现类
        */
       private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
   
       private final Set<String> registeredService = ConcurrentHashMap.newKeySet();
   
       /**
        * 加锁,保证注册的线程安全
        * 注册某个实现类。其实现的接口是反射获取的,无需传入参数
        * @param service 实现类
        * @param <T> 泛型
        */
       @Override
       public synchronized  <T> void register(T service) {
           // 获取规范类名
           String serviceName = service.getClass().getCanonicalName();
           if (registeredService.contains(serviceName)) {
               return;
           }
           registeredService.add(serviceName);
           // 获取该实现类实现的所有接口 的Class对象
           Class[] interfaces = service.getClass().getInterfaces();
           if (interfaces.length == 0) {
               // 注册的服务没有实现任何接口
               throw new RpcException(RpcErrorMessageEnum.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
           }
           for (Class i : interfaces) {
               // 某个被实现的接口 ： 当前实现类
               serviceMap.put(i.getCanonicalName(), service);
           }
           LOGGER.info("Add service: {} and interfaces:{}", serviceName, service.getClass().getInterfaces());
       }
   
   
       /**
        * 线程安全
        * 根据接口获取实现类
        * @param serviceName 接口名
        * @return 实现类
        */
       @Override
       public synchronized Object getService(String serviceName) {
           Object service = serviceMap.get(serviceName);
           if (null == service) {
               throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_FOUND);
           }
           return service;
       }
   }
   ```

4. RpcServer抽象为一个请求监听实例。
   关键就是：`RpcServer`没有和`serviceRegistry`耦合。**RpcServer能够监听的接口调用，是由`serviceRegistry`决定的**。这正是 实现一个监听实例 能够监听多个接口调用的关键。

   ```java
   public class RpcServer {
   
       private static final int CORE_POOL_SIZE = 10;
       private static final int MAXIMUM_POOL_SIZE_SIZE = 100;
       private static final int KEEP_ALIVE_TIME = 1;
       private static final int BLOCKING_QUEUE_CAPACITY = 100;
       // 本地注册中心
       private final ServiceRegistry serviceRegistry;
       // 线程池用于执行方法
       private ExecutorService threadPool;
       // 线程中的方法执行者
       private RpcRequestHandler rpcRequestHandler = new RpcRequestHandler();
   
       private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);
   
       /**
        * 每个Server实例对应单独的线程池、Handler
        * 只需传入注册中心即可，其他的成员都重新构造
        * @param serviceRegistry
        */
       public RpcServer(ServiceRegistry serviceRegistry) {
           this.serviceRegistry = serviceRegistry;
           BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPACITY);
           // 线程工厂
           ThreadFactory threadFactory = Executors.defaultThreadFactory();
           // 每个Server实例对应一个线程池
           this.threadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE_SIZE, KEEP_ALIVE_TIME, TimeUnit.MINUTES, workQueue, threadFactory);
       }
   
       // 一个Server实例对应一个线程池
       public void start(int port) {
   
           try (ServerSocket server = new ServerSocket(port);) {
               LOGGER.info("server starts...");
               Socket socket;
               while ((socket = server.accept()) != null) {
                   LOGGER.info("client connected");
                   threadPool.execute(new RpcRequestHandlerRunnable(socket, rpcRequestHandler, serviceRegistry));
               }
               threadPool.shutdown();
           } catch (IOException e) {
               LOGGER.error("occur IOException:", e);
           }
       }
   }
   ```

## TODO

1. Socket通信效率低，一个线程只能服务于一个请求调用直到调用返回，引入主题：Netty。
2. 换一种序列化方案
3. 移动注册中心逻辑，为后续Nacos作为注册中心作准备



# [v2.0] Netty传输 | Kryo序列化

## 效果预览

![image-20221127193211449](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211271934654.png)

![image-20221127194656949](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211271946638.png)

可以看到，处理请求的都是NIO线程。

## 新增内容

- 使用Netty进行传输
- 使用kryo进行序列化，替代性能低下的JDK原生序列化

## 实现

### kryo序列化

定义序列化接口

```java
public interface Serializer {
    /**
     * 序列化
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化
     * @param bytes 序列化后的字节数组
     * @param clazz 类
     * @param <T>
     * @return 反序列化的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
```

引入kryo进行序列化

```java
/** Kryo 不是线程安全的。每个线程都应该有自己的 Kryo 对象、输入和输出实例。
 * 因此在多线程环境中，可以考虑使用 ThreadLocal 或者对象池来保证线程安全性。
 * ThreadLocal 牺牲空间换取线程安全 ：为每个线程都单独创建本线程专用的 kryo 对象。对于每条线程的每个 kryo 对象来说，都是顺序执行的，因此天然避免了并发安全问题。
 */
public class KryoSerializer implements Serializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KryoSerializer.class);

    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        // 一个ThreadLocal容器中只会有一个kryo实例
        Kryo kryo = new Kryo();
        // 为了提供性能和减小序列化结果体积，提供注册的序列化对象类的方式。
        // 在注册时，会为该序列化类生成 int ID，后续在序列化时使用 int ID 唯一标识该类型
        kryo.register(RpcResponse.class);
        kryo.register(RpcRequest.class);

        kryo.setReferences(false); // 关闭循环引用检测,从而提高一些性能。
        /**
         * 注册会给每一个class一个int类型的Id相关联，这显然比类名称高效
         * 但同时要求反序列化的时候的Id必须与序列化过程中一致。这意味着注册的顺序非常重要。
         */
//        kryo.setRegistrationRequired(false); // 关闭注册行为？？？？？？？？？？？？？？？
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            //将对象序列化为byte数组
            kryo.writeObject(output, obj);
            // kryoThreadLocal调用完之后要remove一下, 防止内存泄露
            KRYO_THREAD_LOCAL.remove();
            return output.toBytes();
        } catch (Exception e) {
            LOGGER.error("occur exception when serialize:", e);
            throw new SerializeException("序列化失败");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            // 从byte数组中反序列化出对对象
            Object o = kryo.readObject(input, clazz);
            KRYO_THREAD_LOCAL.remove();
            // clazz.cast(o) 通过类对象，将o强类型转换
            return clazz.cast(o);
        } catch (Exception e) {
            LOGGER.error("occur exception when deserialize:", e);
            throw new SerializeException("反序列化失败");
        }
    }
}
```

注意：

1. ThreadLocal的remove方法，用完即删，防止内存泄露
2. kryo的api以及背后的原理： setReferences()   register() setReferences()

踩坑：

`kryo`  `DTO`如果[构造方法](https://so.csdn.net/so/search?q=构造方法&spm=1001.2101.3001.7020)被重写，需要手写一个无参构造方法。否则会报错。

### Netty进行传输

#### 自定义的编解码器

继承自Netty的ByteToMessageDecoder，所以可以作为入站处理器

```java
/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:10
 * @description : 继承自Netty的ByteToMessageDecoder，所以可以作为入站处理器
 */
@AllArgsConstructor
public class NettyKryoDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyKryoDecoder.class);

    private Serializer serializer;
    private Class<?> genericClass;

    /**
     * Netty传输的消息长度也就是对象序列化后对应的字节数组的大小，存储在 ByteBuf 头部
     */
    private static final int BODY_LENGTH = 4;

    /**
     * 重写ByteToMessageDecoder的解码方法
     * @param channelHandlerContext
     * @param byteBuf
     * @param list
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {

        //1.byteBuf中写入的消息长度所占的字节数已经是4了，所以 byteBuf 的可读字节必须大于 4。
        if (byteBuf.readableBytes() >= BODY_LENGTH) {
            //2.标记当前readIndex的位置，以便后面重置readIndex 的时候使用
            byteBuf.markReaderIndex();
            //3.读取消息的长度
            //注意： 消息长度是encode的时候我们自己写入的，参见 NettyKryoEncoder 的encode方法
            int dataLength = byteBuf.readInt();
            //4.遇到不合理的情况直接 return
            if (dataLength < 0 || byteBuf.readableBytes() < 0) {
                return;
            }
            //5.如果可读字节数小于消息长度的话，说明是不完整的消息，重置readIndex
            if (byteBuf.readableBytes() < dataLength) {
                byteBuf.resetReaderIndex();
                return;
            }
            // 6.走到这里说明没什么问题了，可以序列化了
            byte[] body = new byte[dataLength];
            byteBuf.readBytes(body);
            // 将bytes数组转换为我们需要的对象
            Object obj = serializer.deserialize(body, genericClass);
            // 传递解码后的结果
            list.add(obj);
        }
    }
}
```

继承MessageToByteEncoder，所以可以作为出站处理器

```java
/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/27 15:06
 * @description : 继承MessageToByteEncoder，所以可以作为出站处理器
 */
@AllArgsConstructor
public class NettyKryoEncoder extends MessageToByteEncoder<Object> {
    private Serializer serializer;
    private Class<?> genericClass;

    /**
     * 将对象转换为字节码然后写入到 ByteBuf 对象中
     * LD格式
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) {
        // 这个方法是Java语言instanceof操作符的动态等效。
        // 形参: Obj -要检查的对象 返回值: 如果obj是该类的实例，则为True
        // 如果收到的object不是指定类型的，就无需编码。指定类型由构造NettyKryoEncoder时指定
        if (genericClass.isInstance(o)) {
            // 1. 将对象转换为byte
            byte[] body = serializer.serialize(o);
            // 2. 读取消息的长度
            int dataLength = body.length;
            // 3.写入消息对应的字节数组长度,writerIndex 加 4
            byteBuf.writeInt(dataLength);
            //4.将字节数组写入 ByteBuf 对象中
            byteBuf.writeBytes(body);
        }
    }
}
```

#### 处理业务的handler

在rpc框架中，业务就是，根据请求中的方法，从注册中心选择实现类调用，返回结果

下面是入站handler，用于调用服务

```java
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerHandler.class);
    // 服务动态调用者
    private static RpcRequestHandler rpcRequestHandler;
    private static ServiceRegistry serviceRegistry;
    static {
        rpcRequestHandler = new RpcRequestHandler();
        serviceRegistry = new DefaultServiceRegistry();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            RpcRequest rpcRequest = (RpcRequest) msg;
            LOGGER.info(String.format("server receive msg: %s", rpcRequest));
            String interfaceName = rpcRequest.getInterfaceName();
            // 从注册中心获取服务实现
            Object service = serviceRegistry.getService(interfaceName);
            // 交由给rpcRequestHandler去反射调用方法
            Object result = rpcRequestHandler.handle(rpcRequest, service);
            LOGGER.info(String.format("server get result: %s", result.toString()));
            // 写会调用结果
            ChannelFuture f = ctx.writeAndFlush(RpcResponse.success(result,rpcRequest.getRequestId()));
            // 写会完成后关闭channel
            f.addListener(ChannelFutureListener.CLOSE);
        } finally {
            /*
            * ReferenceCountUtil.release()其实是ByteBuf.release()方法（从ReferenceCounted接口继承而来）的包装。
            * 从InBound里读取的ByteBuf要手动释放，还有自己创建的ByteBuf要自己负责释放。这两处要调用这个release方法。
            * write Bytebuf到OutBound时由netty负责释放，不需要手动调用release
            * */
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
```

#### 使用Netty建立server

```java
public class NettyRpcServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcServer.class);
    private final int port;
    private Serializer kryoSerializer;


    public NettyRpcServer(int port) {
        this.port = port;
        kryoSerializer = new KryoSerializer();
    }

    public void run() {
        // 用于建立连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 用于处理业务
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // RpcRequest的解码器
                            ch.pipeline().addLast(new NettyKryoDecoder(kryoSerializer, RpcRequest.class));
                            // RpcResponse的编码器
                            ch.pipeline().addLast(new NettyKryoEncoder(kryoSerializer, RpcResponse.class));
                            // 业务的
                            ch.pipeline().addLast(new NettyServerHandler());
                        }
                    })
                    // 设置tcp缓冲区
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            // 绑定端口，同步等待绑定成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("occur exception when start server:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```