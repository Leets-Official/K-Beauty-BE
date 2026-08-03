package com.leets.k_beauty.domain.share.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shares")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Share {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recommendationId;

    @Column(nullable = false, unique = true, length = 36)
    private String shareToken;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Share create(Long recommendationId) {
        Share share = new Share();
        share.recommendationId = recommendationId;
        share.shareToken = UUID.randomUUID().toString();
        return share;
    }
}
