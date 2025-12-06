package site.hexaarch.rpc.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.hexaarch.rpc.annotation.RpcService;

/**
 * 计算器服务实现类
 *
 * @author kenyon chen
 */
@RpcService(CalculatorService.class)
public class CalculatorServiceImpl implements CalculatorService {
    private static final Logger logger = LoggerFactory.getLogger(CalculatorServiceImpl.class);

    @Override
    public int add(int a, int b) {
        int result = a + b;
        logger.info("Calculated: {} + {} = {}", a, b, result);
        return result;
    }

    @Override
    public int subtract(int a, int b) {
        int result = a - b;
        logger.info("Calculated: {} - {} = {}", a, b, result);
        return result;
    }

    @Override
    public int multiply(int a, int b) {
        int result = a * b;
        logger.info("Calculated: {} * {} = {}", a, b, result);
        return result;
    }

    @Override
    public int divide(int a, int b) throws IllegalArgumentException {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为0");
        }
        int result = a / b;
        logger.info("Calculated: {} / {} = {}", a, b, result);
        return result;
    }
}