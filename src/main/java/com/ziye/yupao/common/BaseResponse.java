package com.ziye.yupao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果
 *
 * @author ziye
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {
    /**
     * 状态码
     */
    private int code;

    /**
     * 状态码信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 详情
     */
    private String description;

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(int code, String message) {
        this(code, message, (T) null);
    }

    public BaseResponse(int code, String message, T data, String description) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.description = description;
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null, errorCode.getDescription());
    }

    public BaseResponse(int code, String message, String description) {
        this(code, message, null, description);
    }

    public BaseResponse(ErrorCode errorCode, String message, String description) {
        this(errorCode.getCode(), message, null, description);
    }
}
