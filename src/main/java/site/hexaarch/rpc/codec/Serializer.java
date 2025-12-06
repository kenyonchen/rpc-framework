package site.hexaarch.rpc.codec;

/**
 * 序列化接口，支持不同的序列化方式
 *
 * @author kenyon chen
 */
public interface Serializer {
    /**
     * 将对象序列化为字节数组
     *
     * @param obj 要序列化的对象
     * @param <T> 对象类型
     * @return 序列化后的字节数组
     * @throws Exception 序列化异常
     */
    <T> byte[] serialize(T obj) throws Exception;

    /**
     * 将字节数组反序列化为对象
     *
     * @param bytes 序列化后的字节数组
     * @param clazz 对象类型
     * @param <T>   对象类型
     * @return 反序列化后的对象
     * @throws Exception 反序列化异常
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception;

    /**
     * 序列化类型枚举
     */
    enum Type {
        JSON(1);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        /**
         * 根据code查找对应的序列化类型
         *
         * @param code 序列化类型码
         * @return 序列化类型
         */
        public static Type findByCode(int code) {
            for (Type type : Type.values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return JSON; // 默认使用JSON
        }
    }
}