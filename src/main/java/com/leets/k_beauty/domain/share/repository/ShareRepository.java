package com.leets.k_beauty.domain.share.repository;

import com.leets.k_beauty.domain.share.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShareRepository extends JpaRepository<Share, Long> {

    Optional<Share> findByShareToken(String shareToken);
}
