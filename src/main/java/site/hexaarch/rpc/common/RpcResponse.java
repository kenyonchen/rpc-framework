package site.hexaarch.rpc.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * RPC响应类
 *
 * @author kenyon chen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // 请求ID，与请求对应
    private String requestId;
    // 响应结果
    private T result;
    // 错误信息
    private String error;

    // 无参构造函数
    public RpcResponse() {
    }

    // getter和setter方法
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // 判断是否成功
    public boolean isSuccess() {
        return error == null;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", result=" + result +
                ", error='" + error + '\'' +
                ", isSuccess=" + isSuccess() +
                '}';
    }
}