package com.example.NFC_system.repository;

import com.example.NFC_system.entity.ShareComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareCommentRepository extends JpaRepository<ShareComment, Long> {
    List<ShareComment> findByShareIdAndParentIdIsNullOrderByCreatedAtDesc(Long shareId);
    List<ShareComment> findByParentIdOrderByCreatedAtAsc(Long parentId);
    long countByShareId(Long shareId);
}