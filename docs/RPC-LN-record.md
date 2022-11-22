1. [v0.9]  BIO | 动态代理  | JDK序列化

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

   客户端中，对接口`ServiceTest`的方法调用，不需要new出来其实现类`ServiceTestImpl`，而是调用`rpcClient.sendRpcRequest

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
           RpcClient rpcClient = new RpcClient();
           return rpcClient;
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

   

   # [v0.92]

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