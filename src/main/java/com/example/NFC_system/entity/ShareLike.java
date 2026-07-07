package com.example.NFC_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "share_like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shareId", "userId"})
})
public class ShareLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long shareId;
    private Long userId;

    private LocalDateTime createdAt;

    // ===== getter setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}