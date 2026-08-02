package com.leets.k_beauty.global.exception;

public class SessionNotInProgressException extends BusinessException {

    public SessionNotInProgressException() {
        super(ErrorCode.SESSION_NOT_IN_PROGRESS);
    }
}
