package com.example.NFC_system.repository;

import com.example.NFC_system.entity.CommunityShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityShareRepository extends JpaRepository<CommunityShare, Long> {
    List<CommunityShare> findAllByOrderByCreatedAtDesc();
    Optional<CommunityShare> findByVersionId(Long versionId);
    List<CommunityShare> findTop6ByOrderByCreatedAtDesc();
}