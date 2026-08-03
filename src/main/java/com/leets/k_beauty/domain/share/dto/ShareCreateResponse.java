package com.leets.k_beauty.domain.share.dto;

import com.leets.k_beauty.domain.share.entity.Share;

public record ShareCreateResponse(String shareToken) {

    public static ShareCreateResponse from(Share share) {
        return new ShareCreateResponse(share.getShareToken());
    }
}
