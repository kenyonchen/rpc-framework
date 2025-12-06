package site.hexaarch.rpc.example;

import site.hexaarch.rpc.annotation.RpcService;

/**
 * 计算器服务实现类
 *
 * @author kenyon chen
 */
@RpcService(CalculatorService.class)
public class CalculatorServiceImpl implements CalculatorService {
    @Override
    public int add(int a, int b) {
        int result = a + b;
        System.out.println("Calculated: " + a + " + " + b + " = " + result);
        return result;
    }

    @Override
    public int subtract(int a, int b) {
        int result = a - b;
        System.out.println("Calculated: " + a + " - " + b + " = " + result);
        return result;
    }

    @Override
    public int multiply(int a, int b) {
        int result = a * b;
        System.out.println("Calculated: " + a + " * " + b + " = " + result);
        return result;
    }

    @Override
    public int divide(int a, int b) throws IllegalArgumentException {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为0");
        }
        int result = a / b;
        System.out.println("Calculated: " + a + " / " + b + " = " + result);
        return result;
    }
}