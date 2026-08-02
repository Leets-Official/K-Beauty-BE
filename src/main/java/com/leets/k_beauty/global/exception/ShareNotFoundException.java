package com.leets.k_beauty.global.exception;

public class ShareNotFoundException extends BusinessException {

    public ShareNotFoundException() {
        super(ErrorCode.SHARE_NOT_FOUND);
    }
}
