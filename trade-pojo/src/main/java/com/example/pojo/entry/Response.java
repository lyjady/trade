package com.example.pojo.entry;

/**
 * @author LinYongJin
 * @date 2019/12/1 17:10
 */
public class Response<T> {

    private boolean result;

    private String message;

    private T data;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
