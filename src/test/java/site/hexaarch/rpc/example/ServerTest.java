package site.hexaarch.rpc.example;

import site.hexaarch.rpc.registry.NacosRegistryCenter;
import site.hexaarch.rpc.registry.RegistryCenter;
import site.hexaarch.rpc.server.RpcServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC服务端测试类
 * @author kenyon chen
 */
public class ServerTest {
    private static final Logger logger = LoggerFactory.getLogger(ServerTest.class);
    
    private RegistryCenter registryCenter;
    private RpcServer server;
    private Thread serverThread;
    private int port = 8081; // 将端口作为实例变量
    
    @Before
    public void setUp() throws Exception {
        // 检查系统属性中是否指定了端口
        String portProperty = System.getProperty("server.port");
        if (portProperty != null && !portProperty.isEmpty()) {
            try {
                port = Integer.parseInt(portProperty);
                logger.info("Using port from system property: {}", port);
            } catch (NumberFormatException e) {
                logger.warn("Invalid port specified in system property, using default port 8081");
            }
        }
        
        // 创建Nacos注册中心实例
        logger.info("Creating Nacos registry center...");
        registryCenter = new NacosRegistryCenter("localhost:8848");
        logger.info("Nacos registry center created: {}", registryCenter);

        // 创建RPC服务器
        logger.info("Creating RPC server with registry center...");
        server = new RpcServer(port, registryCenter);
        logger.info("RPC server created: {}", server);
        
        // 创建服务实现类实例
        CalculatorService calculatorService = new CalculatorServiceImpl();
        
        // 注册服务
        logger.info("Registering service...");
        server.registerService(CalculatorService.class, calculatorService);
        logger.info("Service registered.");
        
        // 在独立线程中启动服务器
        serverThread = new Thread(() -> {
            try {
                logger.info("Starting server on port {}...", port);
                server.start();
                logger.info("Server started.");
            } catch (Exception e) {
                logger.error("Failed to start RPC server", e);
            }
        });
        
        serverThread.start();
        
        // 等待服务器启动
        Thread.sleep(2000);
        
        logger.info("RPC server started successfully on port {}", port);
        logger.info("Service registered: {}", CalculatorService.class.getName());
    }
    
    @Test
    public void testServerRunning() throws InterruptedException {
        // 保持服务器运行一段时间用于测试
        logger.info("Server is running, keeping it alive for testing...");
        Thread.sleep(30000); // 保持运行30秒用于测试
    }
    
    @After
    public void tearDown() {
        try {
            // 关闭资源
            if (server != null) {
                server.stop();
            }
            if (registryCenter != null) {
                registryCenter.close();
            }
            
            // 等待服务器线程结束
            if (serverThread != null) {
                serverThread.join(5000); // 最多等待5秒
            }
            
            logger.info("Server stopped.");
        } catch (Exception e) {
            logger.error("Error stopping server", e);
        }
    }
}