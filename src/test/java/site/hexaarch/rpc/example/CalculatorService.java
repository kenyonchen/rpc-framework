package site.hexaarch.rpc.example;

/**
 * 计算器服务接口
 *
 * @author kenyon chen
 */
public interface CalculatorService {
    /**
     * 加法运算
     *
     * @param a 第一个加数
     * @param b 第二个加数
     * @return 两数之和
     */
    int add(int a, int b);

    /**
     * 减法运算
     *
     * @param a 被减数
     * @param b 减数
     * @return 两数之差
     */
    int subtract(int a, int b);

    /**
     * 乘法运算
     *
     * @param a 第一个乘数
     * @param b 第二个乘数
     * @return 两数之积
     */
    int multiply(int a, int b);

    /**
     * 除法运算
     *
     * @param a 被除数
     * @param b 除数
     * @return 两数之商
     * @throws IllegalArgumentException 当除数为0时抛出异常
     */
    int divide(int a, int b) throws IllegalArgumentException;
}