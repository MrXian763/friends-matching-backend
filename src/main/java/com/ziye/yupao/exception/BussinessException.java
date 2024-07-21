package com.ziye.yupao.exception;

import com.ziye.yupao.common.ErrorCode;

/**
 * 自定义业务异常
 * 该类型异常抛出后由全局异常处理器捕获处理
 *
 * @author ziye
 */
public class BussinessException extends RuntimeException {
    private final int code;
    private final String description;

    public BussinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BussinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public BussinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
