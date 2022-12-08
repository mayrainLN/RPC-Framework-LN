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

1. 抽象出Handler对象用于真正执行方法，不再和Runnale耦合

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

```java
public class NettyServerMain {
    public static void main(String[] args) {
        HelloServiceImpl helloService = new HelloServiceImpl();
        ServiceProvider serviceProviderImpl = new ServiceProviderImpl();
        // 手动注册
        serviceProviderImpl.register(helloService);
        NettyRpcServer nettyRpcServer = new NettyRpcServer(5656);
        nettyRpcServer.run();
    }
}
```

![image-20221127193211449](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211271934654.png)

```java
public class NettyClientMain {
    public static void main(String[] args) {
        RpcClient rpcClient = new NettyRpcClient("127.0.0.1", 5656);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello1 = helloService.hello(new Hello("netty信息555", "netty描述555"));
        String hello2 = helloService.hello(new Hello("netty信息666", "netty描述666"));
        System.out.println(hello1);
        System.out.println(hello2);
//        assert "Hello description is netty描述444".equals(hello);
    }
}
```

![image-20221127194656949](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211271946638.png)

可以看到，处理请求的都是NIO线程，使用Netty进行NIO网络传输的目的已经达到。

## 新增内容

- 使用Netty进行传输
- 使用Kryo进行序列化，替代性能低下的JDK原生序列化

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

# [v3.0] 引入Nacos作为注册中心

方法锁取消了

countDownLatch

原子类包装对象

## 效果预览

如图，NettyServerMain1 和 NettyServerMain2注册了两个服务端实例。

![image-20221128170512209](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211281705232.png)

注册中心数据面板，已可以看到提供HelloService服务的存活实例数为2；

![image-20221128170756961](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211281707275.png)

实例详情中已经存在两个不同实例的ip和地址信息：分别位于5656端口和5657端口。

![image-20221128171031582](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211281710615.png)

客户端可以正常地返回调用结果。可以看到是**先从Nacos拿到了服务实例的地址信息，再去连接服务实例的。**

![image-20221128172507160](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211281725571.png)

Socket版本的实现不在此展示。

## 新增内容

- 引入Nacos作为注册中心
- 注册中心的可用性比数据强一致性更宝贵。所以选择了偏重AP的 `Nacos` 而不是 偏重CP的`Zookeeper`。
- 重构了Client端的启动、Channel的获取。
- 补增Socket实现。

## 实现

1. 抽象出每个服务器节点的ServiceMap。其实就是原来的本地注册中心。

   ```java
   public interface ServiceProvider {
   
      <T> void addService(T service);
   
      Object getService(String serviceName);
   
   }
   ```

   实现

   ```java
   public class ServiceProviderImpl implements ServiceProvider {
       public static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderImpl.class);
   
       /**
        * key: 接口类名 （完整类名）
        * value: 实现类的实例对象
        * TODO 当前一个接口只能对应一个实现类
        */
       private static final Map<String, Object> SERVICE_MAP = new ConcurrentHashMap<>();
   
       private static final Set<String> REGISTERED_SERVICE = ConcurrentHashMap.newKeySet();
   
       /**
        * 将服务放入ServiceMap中
        * 为什么又不需要加锁了呢？？
        * @param service 实现类
        * @param <T> 泛型
        */
       @Override
       public <T> void addService(T service) {
           // 获取规范类名
           String serviceName = service.getClass().getCanonicalName();
           if (REGISTERED_SERVICE.contains(serviceName)) {
               return;
           }
           REGISTERED_SERVICE.add(serviceName);
           // 获取该实现类实现的所有接口 的Class对象
           Class[] interfaces = service.getClass().getInterfaces();
           if (interfaces.length == 0) {
               // 注册的服务没有实现任何接口
               throw new RpcException(RpcErrorMessageEnum.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
           }
           for (Class i : interfaces) {
               // 某个被实现的接口 ： 当前实现类
               SERVICE_MAP.put(i.getCanonicalName(), service);
           }
           LOGGER.info("Add serviceImpl: {} to interfaces:{}", serviceName, service.getClass().getInterfaces());
       }
   
   
       /**
        * 根据接口获取实现类
        * @param serviceName 接口名
        * @return 实现类
        */
       @Override
       public Object getService(String serviceName) {
           Object service = SERVICE_MAP.get(serviceName);
           if (null == service) {
               throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_FOUND);
           }
           return service;
       }
   }
   ```

2. 抽象出远程注册中心接口

   ```java
   public interface ServiceRegistry {
       /**
        * 注册服务
        * @param serviceName 注册的服务名称
        * @param inetSocketAddress 服务地址
        */
       void register(String serviceName, InetSocketAddress inetSocketAddress);
   
       /**
        * 查询服务
        * @param serviceName 服务名称
        * @return 服务地址
        */
       InetSocketAddress lookupService(String serviceName);
   }
   ```

3. 依赖与Nacos的客户端，很方便的引入Nacos。仅仅只需要配置地址即可

   ```java
   public class NacosServiceRegistry implements ServiceRegistry {
       private static final Logger LOGGER = LoggerFactory.getLogger(NacosServiceRegistry.class);
   
       // 注册中心地址，先写死成本地吧
       private static final String SERVER_ADDR = "127.0.0.1:8848";
       private static final NamingService namingService;
   
       static {
           try {
               // 注册服务
               namingService = NamingFactory.createNamingService(SERVER_ADDR);
           } catch (NacosException e) {
               LOGGER.error("连接到Nacos时有错误发生: ", e);
               throw new RpcException(RpcErrorMessageEnum.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
           }
       }
   
       /**
        * 注册服务到注册中心
        * @param serviceName 注册的服务名称
        * @param inetSocketAddress 服务地址
        */
       @Override
       public void register(String serviceName, InetSocketAddress inetSocketAddress) {
           try {
               namingService.registerInstance(serviceName, inetSocketAddress.getHostName(), inetSocketAddress.getPort());
           } catch (NacosException e) {
               LOGGER.error("注册服务时有错误发生:", e);
               throw new RpcException(RpcErrorMessageEnum.REGISTER_SERVICE_FAILED);
           }
       }
   
       /**
        * 从nacos注册中心获取服务实例地址
        * @param serviceName 服务名称
        * @return  服务实例地址
        */
       @Override
       public InetSocketAddress lookupService(String serviceName) {
           try {
               // 获取服务实例列表
               List<Instance> instances = namingService.getAllInstances(serviceName);
               if (instances.size() < 1) {
                   throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND,"暂无可用的服务实例");
               }
               /**
                * 后续可以在此添加负载均衡策略
                */
               Instance instance = instances.get(0);
               return new InetSocketAddress(instance.getIp(), instance.getPort());
           } catch (NacosException e) {
               LOGGER.error("获取服务时有错误发生:", e);
           }
           return null;
       }
   }
   ```

5. 更新RpcServer接口

   ```java
   public interface RpcServer {
   
       void start();
   
       void setSerializer(Serializer serializer);
   
       <T> void publishService(Object service, Class<T> serviceClass);
   
   }
   
   ```

6. NettyRpcServer现在依赖于serviceProvider和serviceRegistry了。发布服务时需要发布到本地和远程的注册中心。其他基本无需变动。

   ```java
   @Override
   public <T> void publishService(Object service, Class<T> serviceClass) {
       // 向外提供服务前，要先设置序列化器
       if(serializer == null) {
           LOGGER.error("未设置序列化器");
           throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND);
       }
       // 将服务注册到本地的map，键是动态获取的规范类名
       serviceProvider.addService(service);
       // 将服务注册到远程的注册中心
       serviceRegistry.register(serviceClass.getCanonicalName(), new InetSocketAddress(host, port));
   }
   ```

7. 将客户端的`Channel`和`Bootstrap`分离。配置全在`ChannelProvider`，`NettyRpcClient`只需调用`get`即可方便地获取`Channel`与不同服务端通信。

   ```java
   public class ChannelProvider {
       private static final Logger LOGGER = LoggerFactory.getLogger(ChannelProvider.class);
       private static EventLoopGroup eventLoopGroup;
       private static Bootstrap bootstrap;
       /**
        * 默认最大重试次数
        */
       private static final int MAX_RETRY_COUNT = 5;
       private static Channel channel = null;
       
       static {
           bootstrap = initializeBootstrap();
       }
   
       private static Bootstrap initializeBootstrap() {
           eventLoopGroup = new NioEventLoopGroup();
           Bootstrap bootstrap = new Bootstrap();
           bootstrap.group(eventLoopGroup)
                   .channel(NioSocketChannel.class)
                   //连接的超时时间，超过这个时间还是建立不上的话则代表连接失败
                   .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                   //是否开启 TCP 底层心跳机制
                   .option(ChannelOption.SO_KEEPALIVE, true)
                   //TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                   .option(ChannelOption.TCP_NODELAY, true);
           return bootstrap;
       }
       
   
       /**
        * 获取用于发出请求的Channel
        * @param inetSocketAddress 从注册中心获取到的服务实例的地址
        * @param serializer 序列化器
        * @return 于服务提供端相连的Channel
        */
       public static Channel get(InetSocketAddress inetSocketAddress, Serializer serializer) {
           // 设置handler, 既然有了通信需求，所以就地设置编解码器
           bootstrap.handler(new ChannelInitializer<SocketChannel>() {
               @Override
               protected void initChannel(SocketChannel ch) {
                   /*自定义序列化编解码器*/
                   ch.pipeline().addLast(new NettyKryoDecoder(serializer, RpcResponse.class))
                           .addLast(new NettyKryoEncoder(serializer, RpcRequest.class))
                           .addLast(new NettyClientHandler());
               }
           });
           /**
            * CountDownLatch是一个同步工具类，用来协调多个线程之间的同步，或者说起到线程之间的通信（而不是用作互斥的作用）。
            * 能够使一个线程在等待另外一些线程完成各自工作之后，再继续执行。使用一个计数器进行实现。
            * 计数器初始值为线程的数量。当每一个线程完成自己任务后，计数器的值就会减一。
            * 当计数器的值为0时，表示所有的线程都已经完成一些任务，然后在CountDownLatch上等待的线程就可以恢复执行接下来的任务。
            */
           CountDownLatch countDownLatch = new CountDownLatch(1);
           try {
               /**
                * 注意：执行connect的是Nio线程，所以需要等到连接建立后才能向后执行。
                * 如果都写在一起, 直接写.sync就好了
                */
               connect(bootstrap, inetSocketAddress, countDownLatch);
               // 阻塞直到countDownLatch减为1
               countDownLatch.await();
           } catch (InterruptedException e) {
               LOGGER.error("获取channel时有错误发生:", e);
           }
           return channel;
       }
   
       /**
        * 缺省重试次数时，以MAX_RETRY_COUNT为默认
        * @param bootstrap
        * @param inetSocketAddress
        * @param countDownLatch
        */
       private static void connect(Bootstrap bootstrap, InetSocketAddress inetSocketAddress, CountDownLatch countDownLatch) {
           connect(bootstrap, inetSocketAddress, MAX_RETRY_COUNT, countDownLatch);
       }
   
       /**
        *
        * @param bootstrap
        * @param inetSocketAddress 从注册中心获取到的服务实例的地址
        * @param retry 最大重试次数
        * @param countDownLatch
        */
       private static void connect(Bootstrap bootstrap, InetSocketAddress inetSocketAddress, int retry, CountDownLatch countDownLatch) {
           bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
               if (future.isSuccess()) {
                   LOGGER.info("client connected!");
                   channel = future.channel();
                   // 连接成功了
                   countDownLatch.countDown();
                   return;
               }
               if (retry == 0) {
                   LOGGER.error("connect failed : over max retry！");
                   countDownLatch.countDown();
                   throw new RpcException(RpcErrorMessageEnum.CLIENT_CONNECT_SERVER_FAILURE);
               }
               // 第几次重连
               int order = (MAX_RETRY_COUNT - retry) + 1;
               // 本次重连的间隔递增
               int delay = 1 << order;
               LOGGER.error("{}: connect failed ：retrying for {} times……", new Date(), order);
               bootstrap.config().group().schedule(() -> connect(bootstrap, inetSocketAddress, retry - 1, countDownLatch), delay, TimeUnit
                       .SECONDS);
           });
       }
   }
   ```

8. 改变`Handler`的执行方式。不再由NIO线程就地调用服务，而是交由给线程池。解放NIO线程，让他仅仅专心地处理事件，提高吞吐量。

   ```java
   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) {
       // 交由自定义的线程池netty-server-handler 执行业务
       threadPool.execute(() -> {
           try {
               LOGGER.info("服务器接收到请求: {}", msg);
               Object result = rpcRequestHandler.handle((RpcRequest) msg);
               // 业务处理完，由自建线程池的线程返回结果
               ChannelFuture future = ctx.writeAndFlush(RpcResponse.success(result, ((RpcRequest)msg).getRequestId()));
               future.addListener(ChannelFutureListener.CLOSE);
           } finally {
               ReferenceCountUtil.release(msg);
           }   
       });
   }
   ```

# [v3.1] 服务实时注销 | 优化注册、发现逻辑 

## 效果预览

关闭服务进程时，自动触发回调，释放用于执行业务的线程池。

![image-20221129161928229](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202211291619778.png)

## 新增内容

- 抽离出NacosUtil，服务端和客户端不再强依赖于Nacos。
- 改变服务注册逻辑。服务类一次注册，只注册一个指定的接口。

- 将服务发现、注册分开。

- 新增服务端 关闭自动向nacos注销、关闭线程池。

- 客户端Channel繁忙时释放group。

## 实现

1. 所有与Nacos Server的交互都交给NacosUtil。方便统一管理。

   ```java
   public class NacosUtil {
   
       private static final Logger LOGGER = LoggerFactory.getLogger(NacosUtil.class);
   
       /**
        * 先写死注册中心的地址
        */
       private static final String SERVER_ADDR = "127.0.0.1:8848";
   
       private static final NamingService NACOS_NAMING_SERVICE = getNacosNamingService();
   
       /**
        * 存储本机已经注册的服务名称，用于后续注销服务
        */
       private static final Set<String> REGISTERED_SERVICES_NAMES = new HashSet<>();
   
       /**
        * 本机地址
        */
       private static InetSocketAddress address;
   
       public static NamingService getNacosNamingService() {
           try {
               return NamingFactory.createNamingService(SERVER_ADDR);
           } catch (NacosException e) {
               LOGGER.error("连接到Nacos时有错误发生: ", e);
               throw new RpcException(RpcErrorMessageEnum.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
           }
       }
   
       public static void registerService(String serviceName, InetSocketAddress address) throws NacosException {
           NACOS_NAMING_SERVICE.registerInstance(serviceName, address.getHostName(), address.getPort());
           // 储存本地地址
           NacosUtil.address = address;
           // 存储已经注册的服务
           REGISTERED_SERVICES_NAMES.add(serviceName);
       }
   
       public static List<Instance> getAllInstance(String serviceName) throws NacosException {
           return NACOS_NAMING_SERVICE.getAllInstances(serviceName);
       }
   
       /**
        * 注销本机所有服务：遍历本机已经注册的所有服务，向Nacos发出信息
        */
       public static void clearRegistry() {
           //
           if(!REGISTERED_SERVICES_NAMES.isEmpty() && address != null) {
               String host = address.getHostName();
               int port = address.getPort();
               Iterator<String> iterator = REGISTERED_SERVICES_NAMES.iterator();
               while(iterator.hasNext()) {
                   String serviceName = iterator.next();
                   try {
                       // 向nacos注销本机的服务
                       NACOS_NAMING_SERVICE.deregisterInstance(serviceName, host, port);
                       LOGGER.info("已注销服务: {} @ {}:{}",serviceName,host,port);
                   } catch (NacosException e) {
                       LOGGER.error("注销服务 {} 失败", serviceName, e);
                   }
               }
           }
       }
   }
   ```

2. 服务端调用一次注册方法，**只注册一个指定的接口**，不再是注册实现类实现的所有接口
   将`service` 从`Object` 替换为泛型 。在这里其实Object也无伤大雅。

   ```java
   @Override
   public <T> void publishService(T service, Class<T> serviceClass) {
       // 向外提供服务前，要先设置序列化器
       if (serializer == null) {
           LOGGER.error("未设置序列化器");
           throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND);
       }
       // 将服务注册到本地的map，键是动态获取的规范类名
       serviceProvider.addService(service,serviceClass);
       // 将服务注册到远程的注册中心
       serviceRegistry.register(serviceClass.getCanonicalName(), new InetSocketAddress(host, port));
   }
   ```

3. 将注册与发现分离。客户端只用到发现，服务端只用到注册

   ```java
   public class NacosServiceRegistry implements ServiceRegistry {
   
       private static final Logger LOGGER = LoggerFactory.getLogger(NacosServiceRegistry.class);
   
       /**
        * 注册服务到注册中心
        * @param serviceName 注册的服务名称
        * @param inetSocketAddress 服务地址 ip+端口
        */
       @Override
       public void register(String serviceName, InetSocketAddress inetSocketAddress) {
           try {
               NacosUtil.registerService(serviceName, inetSocketAddress);
           } catch (NacosException e) {
               LOGGER.error("注册服务时有错误发生:", e);
               throw new RpcException(RpcErrorMessageEnum.REGISTER_SERVICE_FAILED);
           }
       }
   }
   ```

   ```java
   public class NacosServiceDiscovery implements ServiceDiscovery {
       private static final Logger LOGGER = LoggerFactory.getLogger(NacosServiceRegistry.class);
   
       /**
        * 从nacos注册中心获取服务实例地址
        * @param serviceName 服务名称
        * @return InetSocketAddress 服务实例地址
        */
       @Override
       public InetSocketAddress lookupService(String serviceName) {
           try {
               List<Instance> instances = NacosUtil.getAllInstance( serviceName);
               if (instances.size() < 1) {
                   throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND, "暂无可用的服务实例");
               }
               /**
                * 后续可以在此添加负载均衡策略
                */
               Instance instance = instances.get(0);
               return new InetSocketAddress(instance.getIp(), instance.getPort());
           } catch (NacosException e) {
               LOGGER.error("获取服务时有错误发生:", e);
           }
           return null;
       }
   }
   ```

4. 服务实例宕机时要销毁所有线程池

   ```java
   public static void shutDownAll() {
           LOGGER.info("关闭所有线程池...");
           threadPoolsMap.entrySet().parallelStream().forEach(entry -> {
               ExecutorService executorService = entry.getValue();
               executorService.shutdown();
               LOGGER.info("关闭线程池 [{}] [{}]", entry.getKey(), executorService.isTerminated());
               try {
                   executorService.awaitTermination(10, TimeUnit.SECONDS);
               } catch (InterruptedException ie) {
                   LOGGER.error("关闭线程池失败！");
                   executorService.shutdownNow();
               }
           });
    }
   ```

   添加钩子

   ```java
   public class ShutdownHook {
   
       private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);
   
       public static void addClearAllHook() {
           LOGGER.info("启动服务端自动注销服务功能,关闭后将自动注销所有服务、释放线程池");
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
   ```

   



# [v3.2 新增Jackson序列化 | 客户端使用CompetableFuture接受结果 ]

## record

CompletableFuture详解
https://juejin.cn/post/6970558076642394142

## 效果预览

服务端使用JSON序列化

```java
public class NettyServerMain {
    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        // 向注册中心注册本机地址
        NettyRpcServer nettyRpcServer = new NettyRpcServer("127.0.0.1", 5656, Serializer.JSON_SERIALIZER);
        // 发布本机服务
        nettyRpcServer.publishService(helloService, HelloService.class);
        nettyRpcServer.start();
    }
}
```

客户端使用JSON序列化

```java
public class NettyClientMain {
    public static void main(String[] args) {
        RpcClient client = new NettyRpcClient(Serializer.JSON_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        // 获取代理的service实例对象
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        Hello object = new Hello("1111", "1111");
        String res = helloService.hello(object);
        System.out.println(res);
    }
}
```

## 新增内容

- 增加JSON序列化器

-  客户端使用`ConcurrentHashMap`存储已经被发出但未被响应的请求，不再是像如下这样放在channel.attr(key)上。使用CompetableFuture接受结果

  ```java
   AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse" + rpcRequest.getRequestId());
   RpcResponse rpcResponse = channel.attr(key).get();



## 实现

1. JSON序列化器

   ```xml
   <dependency>
       <groupId>com.fasterxml.jackson.core</groupId>
       <artifactId>jackson-databind</artifactId>
       <version>${jackson.version}</version>
   </dependency>
   ```

   ```java
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
   ```

2. 未响应请求的容器

   ```java
   public class UnprocessedRequests {
       private static ConcurrentHashMap<String, CompletableFuture<RpcResponse>> unprocessedResponseFutures = new ConcurrentHashMap<>();
   
       public void put(String requestId, CompletableFuture<RpcResponse> future) {
           unprocessedResponseFutures.put(requestId, future);
       }
   
       public void remove(String requestId) {
           unprocessedResponseFutures.remove(requestId);
       }
   
       /**
        * 返回结果后调用次方法
        * @param rpcResponse
        */
       public void complete(RpcResponse rpcResponse) {
           CompletableFuture<RpcResponse> future = unprocessedResponseFutures.remove(rpcResponse.getRequestId());
           if (null != future) {
               // 将response放入future
               future.complete(rpcResponse);
           } else {
               throw new IllegalStateException();
           }
       }
   }
   ```

3. 客户端逻辑

   ```java
   public class NettyRpcClient implements RpcClient {
       private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcClient.class);
   
       private final Serializer serializer;
   
       private static final EventLoopGroup GROUP;
   
       private static final Bootstrap BOOTSTRAP;
   
       private static final int DEFAULT_SERIALIZER_CODE = 0;
   
       /**
        * 存放客户端尚未得到响应的请求
        */
       private final UnprocessedRequests unprocessedRequests;
       /**
        * 远程Nacos注册中心
        */
       private final ServiceDiscovery serviceDiscovery;
   
       static {
           GROUP = new NioEventLoopGroup();
           BOOTSTRAP = new Bootstrap();
           BOOTSTRAP.group(GROUP)
                   .channel(NioSocketChannel.class)
                   .option(ChannelOption.SO_KEEPALIVE, true);
       }
   
       // 默认使用Kryo序列化
       public NettyRpcClient() {
           this(DEFAULT_SERIALIZER_CODE);
       }
   
       public NettyRpcClient(int code) {
           serviceDiscovery = new NacosServiceDiscovery();
           serializer = Serializer.getSerializer(code);
           unprocessedRequests = new UnprocessedRequests();
       }
   
       /**
        * 发送消息, 返回包装RpcResponse的CompletableFuture
        * @param rpcRequest 消息体
        * @return 服务端返回的数据
        */
       @Override
       public CompletableFuture<RpcResponse> sendRpcRequest(RpcRequest rpcRequest) {
           if (serializer == null) {
               LOGGER.error("未设置序列化器");
               throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND);
           }
           CompletableFuture<RpcResponse> resultFuture = new CompletableFuture<>();
           try {
               // 从注册中心获取服务实例地址
               InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest.getInterfaceName());
               // 获取连接到服务实例的channel
               Channel channel = null;
               try {
                   channel = ChannelProvider.get(inetSocketAddress, serializer);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               if (!channel.isActive()) {
                   GROUP.shutdownGracefully();
                   return null;
   
               }
               // 记录还未被响应的请求
               unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
               // 给writeAndFlush方法返回的ChannelFuture对象添加监听器
               channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) future1 -> {
                   if (future1.isSuccess()) {
                       LOGGER.info("客户端发送消息: {}", rpcRequest.toString());
                   } else {
                       future1.channel().close();
                       resultFuture.completeExceptionally(future1.cause());
                       LOGGER.error("发送消息时有错误发生: ", future1.cause());
                   }
               });
           } catch (RuntimeException e) {
               // 清除请求
               unprocessedRequests.remove(rpcRequest.getRequestId());
               LOGGER.error(e.getMessage(), e);
               /**
                * 当你捕获InterruptException并吞下它时，你基本上阻止任何更高级别的方法/线程组注意到中断。
                * 这可能会导致问题。
                * 通过调用Thread.currentThread().interrupt()
                * 设置线程的中断标志，因此更高级别的中断处理程序会注意到它并且可以正确处理它。
                */
               Thread.currentThread().interrupt();
           }
           // 返回future
           return resultFuture;
       }
   }
   ```

   发起请求的代理方法：

   ```java
   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
       RpcRequest rpcRequest = RpcRequest.builder()
               .interfaceName(method.getDeclaringClass().getName())
               .methodName(method.getName())
               .paramTypes(method.getParameterTypes())
               //BUG 傻逼 不是method.getParameters()
               .parameters(args)
               // 生成请求ID
               .requestId(UUID.randomUUID().toString())
               .build();
       // 代理过程中获得一个rpcClient的实例, 调用实例的sendRpcRequest方法
       Object result = null;
       // 返回的其实是future
       CompletableFuture<RpcResponse> completableFuture = (CompletableFuture<RpcResponse>) rpcClient.sendRpcRequest(rpcRequest);
       try {
           // 阻塞直到handler向future中放入结果
           result = completableFuture.get().getData();
       } catch (InterruptedException | ExecutionException e) {
           LOGGER.error("方法调用请求发送失败", e);
           return null;
       }
       return result;
   }
   ```

# [v3.2 新增负载均衡器：轮询、随机选择ServiceProvider ]

## 效果预览

```java
public class NettyClientMain {
    public static void main(String[] args) {
        RpcClient client = new NettyRpcClient(Serializer.JSON_SERIALIZER,new RoundRobinLoadBalancer());
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        // 获取代理的service实例对象
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        Hello object = new Hello("1111", "1111");
        String res = helloService.hello(object);
        System.out.println(res);

        String res2 = helloService.hello(new Hello("222", "222"));
        System.out.println(res2);
    }
}
```

![image-20221208235107189](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202212082351273.png)

![image-20221208235125432](https://raw.githubusercontent.com/mayrainLN/picGo/main/img/202212082351647.png)

可以看到，Client使用轮询负载均衡器，发出两个请求，请求平均摊送到了两个ServiceProvider实例上

## 实现

负载均衡器接口

```java
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
```

两种实现

```java
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
```

```java
public class RoundRobinLoadBalancer implements LoadBalancer {

    private int index = 0;

    /**
     * @param instances 注册在Nacos的节点实例列表
     * @return 轮询选择出的节点
     */
    @Override
    public Instance select(List<Instance> instances) {
        if(index == instances.size()) {
            index = index % instances.size();
        }
        return instances.get(index++);
    }
}
```

两种负载均衡器实现起来非常简单。插入服务实例List，按照策略进行选择即可。

要强调的是，负载均衡是在客户端完成的（想一想，是理所当然的事情）。

```java
/**
 * 从nacos注册中心获取服务实例地址
 * @param serviceName 服务名称
 * @return InetSocketAddress 服务实例地址
 */
@Override
public InetSocketAddress lookupService(String serviceName) {
    try {
        List<Instance> instances = NacosUtil.getAllInstance(serviceName);
        if (instances.size() < 1) {
            throw new RpcException(RpcErrorMessageEnum.SERIALIZER_NOT_FOUND, "暂无可用的服务实例");
        }
        // 负载均衡
        Instance instance = loadBalancer.select(instances);
        return new InetSocketAddress(instance.getIp(), instance.getPort());
    } catch (NacosException e) {
        LOGGER.error("获取服务时有错误发生:", e);
    }
    return null;
}
```

查看Nacos客户端源码，发现Nacos客户端本地已经有了服务实例的缓存map。不用我们自己再添加了。