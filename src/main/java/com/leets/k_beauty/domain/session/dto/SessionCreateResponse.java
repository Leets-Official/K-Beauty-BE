package com.leets.k_beauty.domain.session.dto;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.enums.SessionStatus;

import java.time.LocalDateTime;

public record SessionCreateResponse(
        String sessionToken,
        SessionStatus status,
        LocalDateTime createdAt
) {
    public static SessionCreateResponse from(Session session, String rawToken) {
        return new SessionCreateResponse(
                rawToken,
                session.getStatus(),
                session.getCreatedAt()
        );
    }
}
