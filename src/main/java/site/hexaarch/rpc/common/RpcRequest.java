package site.hexaarch.rpc.common;

import java.io.Serializable;
import java.util.UUID;

/**
 * RPC请求类
 *
 * @author kenyon chen
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // 请求ID，用于唯一标识一次请求
    private String requestId;
    // 服务名称
    private String serviceName;
    // 方法名称
    private String methodName;
    // 参数类型数组
    private Class<?>[] parameterTypes;
    // 参数值数组
    private Object[] parameters;

    public RpcRequest() {
        this.requestId = UUID.randomUUID().toString();
    }

    // getter和setter方法
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId='" + requestId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + (parameterTypes != null ? parameterTypes.length : 0) +
                ", parameters=" + (parameters != null ? parameters.length : 0) +
                '}';
    }
}