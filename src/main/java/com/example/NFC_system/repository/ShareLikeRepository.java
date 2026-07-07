package com.example.NFC_system.repository;

import com.example.NFC_system.entity.ShareLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShareLikeRepository extends JpaRepository<ShareLike, Long> {
    Optional<ShareLike> findByShareIdAndUserId(Long shareId, Long userId);
    long countByShareId(Long shareId);
    boolean existsByShareIdAndUserId(Long shareId, Long userId);
}