package com.example.NFC_system.controller;

import com.example.NFC_system.entity.*;
import com.example.NFC_system.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Autowired
    private CommunityShareRepository shareRepository;

    @Autowired
    private ShareCommentRepository commentRepository;

    @Autowired
    private ShareLikeRepository likeRepository;

    @Autowired
    private CharacterVersionRepository versionRepository;

    // Share a version to community
    @PostMapping("/share")
    public CommunityShare shareVersion(
            @RequestParam Long versionId,
            @RequestParam Long userId,
            @RequestParam String authorName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String posterImageUrl
    ) {
        // Check if already shared
        Optional<CommunityShare> existing = shareRepository.findByVersionId(versionId);
        if (existing.isPresent()) {
            // Update existing share
            CommunityShare share = existing.get();
            share.setDescription(description);
            share.setTags(tags);
            share.setAuthorName(authorName);
            if (posterImageUrl != null && !posterImageUrl.isEmpty()) {
                share.setPosterImageUrl(posterImageUrl);
            }
            return shareRepository.save(share);
        }

        CommunityShare share = new CommunityShare();
        share.setVersionId(versionId);
        share.setUserId(userId);
        share.setAuthorName(authorName);
        share.setDescription(description);
        share.setTags(tags);
        share.setPosterImageUrl(posterImageUrl);
        share.setRating(0);
        share.setLikesCount(0);
        share.setCommentsCount(0);
        share.setCreatedAt(LocalDateTime.now());
        return shareRepository.save(share);
    }

    // Remove share
    @DeleteMapping("/unshare")
    public Map<String, Object> unshare(@RequestParam Long versionId) {
        Map<String, Object> result = new HashMap<>();
        Optional<CommunityShare> existing = shareRepository.findByVersionId(versionId);
        if (existing.isPresent()) {
            shareRepository.delete(existing.get());
            result.put("success", true);
        } else {
            result.put("success", false);
            result.put("message", "Not shared");
        }
        return result;
    }

    // Check if a version is shared
    @GetMapping("/isShared")
    public Map<String, Object> isShared(@RequestParam Long versionId) {
        Map<String, Object> result = new HashMap<>();
        Optional<CommunityShare> existing = shareRepository.findByVersionId(versionId);
        result.put("shared", existing.isPresent());
        if (existing.isPresent()) {
            result.put("shareId", existing.get().getId());
        }
        return result;
    }

    // List all shares (with version data)
    @GetMapping("/list")
    public List<Map<String, Object>> listShares() {
        List<CommunityShare> shares = shareRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (CommunityShare share : shares) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", share.getId());
            item.put("versionId", share.getVersionId());
            item.put("userId", share.getUserId());
            item.put("authorName", share.getAuthorName());
            item.put("description", share.getDescription());
            item.put("tags", share.getTags());
            item.put("rating", share.getRating());
            item.put("likesCount", share.getLikesCount());
            item.put("commentsCount", share.getCommentsCount());
            item.put("posterImageUrl", share.getPosterImageUrl());
            item.put("createdAt", share.getCreatedAt());

            // Attach version preview data
            versionRepository.findById(share.getVersionId()).ifPresent(v -> {
                item.put("previewImageUrl", v.getPreviewImageUrl());
                item.put("modelUrl", v.getModelUrl());
                item.put("has3D", v.getModelUrl() != null && !v.getModelUrl().isEmpty());
            });

            result.add(item);
        }
        return result;
    }

    // Recent shares for home preview
    @GetMapping("/recent")
    public List<Map<String, Object>> recentShares() {
        List<CommunityShare> shares = shareRepository.findTop6ByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (CommunityShare share : shares) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", share.getId());
            item.put("versionId", share.getVersionId());
            item.put("authorName", share.getAuthorName());
            item.put("description", share.getDescription());
            item.put("likesCount", share.getLikesCount());
            item.put("tags", share.getTags());
            item.put("posterImageUrl", share.getPosterImageUrl());
            item.put("createdAt", share.getCreatedAt());

            versionRepository.findById(share.getVersionId()).ifPresent(v -> {
                item.put("previewImageUrl", v.getPreviewImageUrl());
                item.put("modelUrl", v.getModelUrl());
                item.put("has3D", v.getModelUrl() != null && !v.getModelUrl().isEmpty());
            });

            result.add(item);
        }
        return result;
    }

    // Get share detail
    @GetMapping("/detail")
    public Map<String, Object> getDetail(@RequestParam Long shareId, @RequestParam(required = false) Long userId) {
        CommunityShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", share.getId());
        result.put("versionId", share.getVersionId());
        result.put("userId", share.getUserId());
        result.put("authorName", share.getAuthorName());
        result.put("description", share.getDescription());
        result.put("tags", share.getTags());
        result.put("rating", share.getRating());
        result.put("likesCount", share.getLikesCount());
        result.put("commentsCount", share.getCommentsCount());
        result.put("posterImageUrl", share.getPosterImageUrl());
        result.put("createdAt", share.getCreatedAt());

        // Check if current user liked
        if (userId != null) {
            result.put("liked", likeRepository.existsByShareIdAndUserId(shareId, userId));
        } else {
            result.put("liked", false);
        }

        // Attach version data (full parameters)
        versionRepository.findById(share.getVersionId()).ifPresent(v -> {
            result.put("previewImageUrl", v.getPreviewImageUrl());
            result.put("modelUrl", v.getModelUrl());
            result.put("designImageUrl", v.getDesignImageUrl());
            result.put("poseImageUrl", v.getPoseImageUrl());
            result.put("extraImageUrl", v.getExtraImageUrl());
            result.put("has3D", v.getModelUrl() != null && !v.getModelUrl().isEmpty());

            // Prompts
            result.put("characterPrompt", v.getCharacterPrompt());
            result.put("scenePrompt", v.getScenePrompt());
            result.put("actionPrompt", v.getActionPrompt());

            // Expression parameters
            result.put("expression1", v.getExpression1());
            result.put("expression2", v.getExpression2());
            result.put("expression3", v.getExpression3());
            result.put("expression4", v.getExpression4());
            result.put("expression5", v.getExpression5());
            result.put("expression6", v.getExpression6());
            result.put("expression7", v.getExpression7());

            // Audio
            result.put("audioUrl", v.getAudioUrl());

            // Status & meta
            result.put("versionStatus", v.getStatus());
            result.put("characterId", v.getCharacterId());
        });

        return result;
    }

    // Toggle like
    @PostMapping("/like")
    public Map<String, Object> toggleLike(@RequestParam Long shareId, @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        Optional<ShareLike> existing = likeRepository.findByShareIdAndUserId(shareId, userId);

        CommunityShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            share.setLikesCount(Math.max(0, share.getLikesCount() - 1));
            shareRepository.save(share);
            result.put("liked", false);
        } else {
            ShareLike like = new ShareLike();
            like.setShareId(shareId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            likeRepository.save(like);
            share.setLikesCount(share.getLikesCount() + 1);
            shareRepository.save(share);
            result.put("liked", true);
        }

        result.put("likesCount", share.getLikesCount());
        return result;
    }

    // Add comment
    @PostMapping("/comment")
    public ShareComment addComment(
            @RequestParam Long shareId,
            @RequestParam Long userId,
            @RequestParam String authorName,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId
    ) {
        ShareComment comment = new ShareComment();
        comment.setShareId(shareId);
        comment.setUserId(userId);
        comment.setAuthorName(authorName);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setLikesCount(0);
        comment.setCreatedAt(LocalDateTime.now());

        ShareComment saved = commentRepository.save(comment);

        // Update comment count
        CommunityShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));
        share.setCommentsCount((int) commentRepository.countByShareId(shareId));
        shareRepository.save(share);

        return saved;
    }

    // Get comments for a share (with replies)
    @GetMapping("/comments")
    public List<Map<String, Object>> getComments(@RequestParam Long shareId) {
        List<ShareComment> topLevel = commentRepository.findByShareIdAndParentIdIsNullOrderByCreatedAtDesc(shareId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ShareComment c : topLevel) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("authorName", c.getAuthorName());
            item.put("userId", c.getUserId());
            item.put("content", c.getContent());
            item.put("likesCount", c.getLikesCount());
            item.put("createdAt", c.getCreatedAt());

            // Get replies
            List<ShareComment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId());
            List<Map<String, Object>> replyList = new ArrayList<>();
            for (ShareComment r : replies) {
                Map<String, Object> ri = new HashMap<>();
                ri.put("id", r.getId());
                ri.put("authorName", r.getAuthorName());
                ri.put("userId", r.getUserId());
                ri.put("content", r.getContent());
                ri.put("likesCount", r.getLikesCount());
                ri.put("createdAt", r.getCreatedAt());
                replyList.add(ri);
            }
            item.put("replies", replyList);

            result.add(item);
        }
        return result;
    }

    // Like a comment
    @PostMapping("/comment/like")
    public Map<String, Object> likeComment(@RequestParam Long commentId) {
        Map<String, Object> result = new HashMap<>();
        ShareComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
        result.put("likesCount", comment.getLikesCount());
        return result;
    }

    // Rate a share
    @PostMapping("/rate")
    public Map<String, Object> rateShare(@RequestParam Long shareId, @RequestParam Integer rating) {
        Map<String, Object> result = new HashMap<>();
        CommunityShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));
        share.setRating(rating);
        shareRepository.save(share);
        result.put("rating", rating);
        return result;
    }
}