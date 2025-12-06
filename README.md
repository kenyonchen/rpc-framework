# 手搓RPC框架

这是一个基于Java实现的简单RPC框架，参考Dubbo的设计思路，实现了服务注册与发现、网络传输、序列化等核心功能。

## 项目结构

```
rpc-framework/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── site/
│   │           └── hexaarch/
│   │               └── rpc/
│   │                   ├── annotation/    # 注解定义
│   │                   ├── codec/         # 序列化模块
│   │                   ├── common/        # 通用类和接口
│   │                   ├── registry/      # 服务注册中心
│   │                   ├── transport/     # 网络传输模块
│   │                   ├── client/        # 客户端核心
│   │                   ├── server/        # 服务端核心
│   │                   └── loadbalance/   # 负载均衡策略
│   └── test/
│       └── java/
│           └── site/
│               └── hexaarch/
│                   └── rpc/
│                       ├── example/       # 测试示例代码
│                       └── ...            # 其他测试代码
├── pom.xml                                # Maven配置文件
└── README.md                              # 项目说明文档
```

## 核心功能

1. **服务注册与发现**：支持ZooKeeper和Nacos作为服务注册中心，支持服务的注册、发现和订阅。
2. **网络传输**：基于Netty实现高效的网络通信，支持TCP长连接和异步通信。
3. **序列化**：支持JSON序列化，可扩展其他序列化方式。
4. **服务代理**：基于Java动态代理实现远程服务调用。
5. **负载均衡**：支持随机和轮询两种负载均衡算法。
6. **异常处理**：完善的异常处理机制，提供详细的错误信息。
7. **日志系统**：集成SLF4J和Logback，提供统一的日志管理。

## 技术栈

- Java 11+
- Netty 4.1.x
- ZooKeeper 3.8.x
- Nacos 2.1.x
- Jackson 2.13.x
- SLF4J 1.7.x
- Logback 1.2.x
- Maven 3.x
- JUnit 4.13.x

## 依赖安装

1. 确保已安装Java 11或更高版本
2. 确保已安装Maven 3.x
3. 确保已安装并启动以下任一注册中心：
   - ZooKeeper（默认端口2181）
   - Nacos（默认端口8848）

## 使用方法

### 1. 定义服务接口

```java
public interface CalculatorService {
    int add(int a, int b);
    int subtract(int a, int b);
    int multiply(int a, int b);
    int divide(int a, int b) throws IllegalArgumentException;
}
```

### 2. 实现服务接口

```java
@RpcService(CalculatorService.class)
public class CalculatorServiceImpl implements CalculatorService {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
    // 其他方法实现...
}
```

### 3. 服务端示例

```java
public class ServerExample {
    public static void main(String[] args) {
        // 使用Nacos作为注册中心
        RegistryCenter registryCenter = new NacosRegistryCenter("localhost:8848");
        // 或者使用ZooKeeper作为注册中心
        // RegistryCenter registryCenter = new ZookeeperRegistryCenter("localhost:2181");
        
        int port = 8081;
        RpcServer server = new RpcServer(port, registryCenter);
        
        CalculatorService calculatorService = new CalculatorServiceImpl();
        server.registerService(CalculatorService.class, calculatorService);
        
        server.start();
    }
}
```

### 4. 客户端示例

```java
public class ClientExample {
    public static void main(String[] args) {
        // 使用Nacos作为注册中心
        RegistryCenter registryCenter = new NacosRegistryCenter("localhost:8848");
        // 或者使用ZooKeeper作为注册中心
        // RegistryCenter registryCenter = new ZookeeperRegistryCenter("localhost:2181");
        
        // 使用轮询负载均衡策略
        RoundRobinLoadBalance loadBalance = new RoundRobinLoadBalance();
        CalculatorService calculatorService = (CalculatorService) Proxy.newProxyInstance(
            CalculatorService.class.getClassLoader(),
            new Class<?>[]{CalculatorService.class},
            new ServiceProxy(CalculatorService.class, registryCenter, loadBalance)
        );
        
        // 调用远程服务
        int result = calculatorService.add(10, 5);
        System.out.println("10 + 5 = " + result);
    }
}
```

## 运行示例
### 克隆项目
```bash
git clone https://github.com/kenyonchen/rpc-framework.git
```

### 单实例运行

1. 启动注册中心服务（ZooKeeper或Nacos）
2. 编译项目
   ```bash
   cd rpc-framework
   mvn clean compile test-compile
   ```
3. 运行服务端测试
   ```bash
   mvn test -Dtest=site.hexaarch.rpc.example.ServerTest
   ```
4. 在另一个终端运行客户端测试
   ```bash
   mvn test -Dtest=site.hexaarch.rpc.example.ClientTest
   ```

### 多实例负载均衡测试

1. 启动注册中心服务（推荐使用Nacos）
2. 编译项目
   ```bash
   cd rpc-framework
   mvn clean compile test-compile
   ```
3. 启动多个服务实例
   ```bash
   ./start-multiple-servers.sh
   ```
4. 运行客户端测试
   ```bash
   mvn test -Dtest=site.hexaarch.rpc.example.ClientTest#testMultipleCallsWithLoadBalance
   ```
5. 停止服务实例
   ```bash
   ./stop-servers.sh
   ```

## 扩展点

1. **序列化扩展**：可自行实现`Serializer`接口，完成新的序列化方式的添加。
2. **负载均衡扩展**：可自行实现`LoadBalance`接口，完成新的负载均衡算法的支持。
3. **注册中心扩展**：可自行实现`RegistryCenter`接口，完成对其他注册中心的支持。
4. **网络传输扩展**：可自行实现`TransportClient`和`TransportServer`接口，完成对其他传输协议的支持。

## 注意事项

1. 本框架仅供学习和参考，不建议直接用于生产环境。
2. 使用前请确保注册中心服务已启动。
3. 如需修改配置，请调整相应的代码参数。
4. 多实例测试时，请确保各实例使用不同端口。

## 许可证

MIT License