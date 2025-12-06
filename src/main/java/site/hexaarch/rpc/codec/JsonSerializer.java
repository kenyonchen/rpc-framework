package site.hexaarch.rpc.codec;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON序列化实现
 *
 * @author kenyon chen
 */
public class JsonSerializer implements Serializer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T obj) throws Exception {
        if (obj == null) {
            return new byte[0];
        }
        return objectMapper.writeValueAsBytes(obj);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return objectMapper.readValue(bytes, clazz);
    }
}