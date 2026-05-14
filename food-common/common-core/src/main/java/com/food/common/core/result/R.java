package com.food.common.core.result;

import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return restResult(null, 200, "success");
    }

    public static <T> R<T> ok(T data) {
        return restResult(data, 200, "success");
    }

    public static <T> R<T> ok(T data, String message) {
        return restResult(data, 200, message);
    }

    public static <T> R<T> fail() {
        return restResult(null, 500, "fail");
    }

    public static <T> R<T> fail(String message) {
        return restResult(null, 500, message);
    }

    public static <T> R<T> fail(int code, String message) {
        return restResult(null, code, message);
    }

    private static <T> R<T> restResult(T data, int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}
