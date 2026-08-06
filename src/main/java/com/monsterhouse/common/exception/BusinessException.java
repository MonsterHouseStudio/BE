package com.monsterhouse.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object[] args;
    public BusinessException(ErrorCode errorCode){
        this(errorCode, new Object[0]);
    }
    public BusinessException(ErrorCode errorCode, Object... args){
        super(errorCode.name());
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
    }
}
