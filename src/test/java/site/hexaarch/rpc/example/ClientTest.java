package site.hexaarch.rpc.example;

import site.hexaarch.rpc.client.ServiceProxy;
import site.hexaarch.rpc.loadbalance.RoundRobinLoadBalance;
import site.hexaarch.rpc.registry.NacosRegistryCenter;
import site.hexaarch.rpc.registry.RegistryCenter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

/**
 * RPC客户端测试类
 * @author kenyon chen
 */
public class ClientTest {
    private static final Logger logger = LoggerFactory.getLogger(ClientTest.class);
    
    private RegistryCenter registryCenter;
    private CalculatorService calculatorService;

    @Before
    public void setUp() throws Exception {
        // 创建注册中心
        logger.info("Creating Nacos registry center...");
        registryCenter = new NacosRegistryCenter("localhost:8848");
        logger.info("Nacos registry center created: {}", registryCenter);
        
        // 创建服务代理，使用轮询负载均衡策略
        logger.info("Creating service proxy with round robin load balance...");
        RoundRobinLoadBalance loadBalance = new RoundRobinLoadBalance();
        calculatorService = (CalculatorService) Proxy.newProxyInstance(
            CalculatorService.class.getClassLoader(),
            new Class<?>[]{CalculatorService.class},
            new ServiceProxy(CalculatorService.class, registryCenter, loadBalance)
        );
        
        logger.info("RPC client initialized successfully");
    }

    @Test
    public void testCalculatorServiceAdd() throws Exception {
        logger.info("--- Testing Calculator Service Add ---");
        
        // 测试加法
        int result = calculatorService.add(10, 5);
        assertEquals(15, result);
        logger.info("10 + 5 = {}", result);
    }
    
    @Test
    public void testCalculatorServiceSubtract() throws Exception {
        logger.info("--- Testing Calculator Service Subtract ---");
        
        // 测试减法
        int result = calculatorService.subtract(10, 5);
        assertEquals(5, result);
        logger.info("10 - 5 = {}", result);
    }
    
    @Test
    public void testCalculatorServiceMultiply() throws Exception {
        logger.info("--- Testing Calculator Service Multiply ---");
        
        // 测试乘法
        int result = calculatorService.multiply(10, 5);
        assertEquals(50, result);
        logger.info("10 * 5 = {}", result);
    }
    
    @Test
    public void testCalculatorServiceDivide() throws Exception {
        logger.info("--- Testing Calculator Service Divide ---");
        
        // 测试除法
        int result = calculatorService.divide(10, 5);
        assertEquals(2, result);
        logger.info("10 / 5 = {}", result);
    }
    
    @Test
    public void testMultipleCallsWithLoadBalance() throws Exception {
        logger.info("--- Testing Calculator Service with Load Balance ---");
        
        // 多次调用以测试负载均衡
        for (int i = 0; i < 5; i++) {
            // 测试加法
            int result = calculatorService.add(10, 5);
            assertEquals(15, result);
            logger.info("Round {}: 10 + 5 = {}", i+1, result);
            
            // 稍微延时以便观察负载均衡效果
            Thread.sleep(500);
        }
        
        logger.info("All RPC calls completed successfully");
    }

    @After
    public void tearDown() {
        // 关闭资源
        if (registryCenter != null) {
            registryCenter.close();
        }
    }
}