package org.example.smartlearning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.CreatePostRequest;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.dto.response.CommentResponse;
import org.example.smartlearning.dto.response.CommunityPostResponse;
import org.example.smartlearning.dto.response.PostDetailResponse;
import org.example.smartlearning.service.community.CommentService;
import org.example.smartlearning.service.community.InteractionService;
import org.example.smartlearning.service.community.PostService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社区控制器
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final PostService postService;
    private final CommentService commentService;
    private final InteractionService interactionService;

    // ==================== 帖子 ====================

    @PostMapping("/posts")
    public ApiResponse<CommunityPostResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success("发布成功",
                postService.createPost(userId, request.getTitle(), request.getContent(),
                        request.getSubjectId(), request.getAnonymous()));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> updatePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId,
            @RequestBody CreatePostRequest request) {
        return ApiResponse.success("编辑成功",
                postService.updatePost(userId, postId, request.getTitle(), request.getContent()));
    }

    @GetMapping("/public/posts")
    public ApiResponse<Page<CommunityPostResponse>> getPublicPosts(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @RequestParam(value = "keyword", required = false) String keyword,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(postService.getPublicPosts(subjectId, page, size, sort, userId, keyword));
    }

    @GetMapping("/public/posts/{postId}")
    public ApiResponse<PostDetailResponse> getPostDetail(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(postService.getPostDetail(postId, userId));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId) {
        postService.deletePost(userId, postId);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/my/posts")
    public ApiResponse<List<CommunityPostResponse>> getMyPosts(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(postService.getUserPosts(userId));
    }

    // ==================== 评论 ====================

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponse> addComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam("content") String content) {
        return ApiResponse.success(commentService.addComment(userId, postId, parentId, content));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId) {
        commentService.deleteComment(userId, commentId);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/public/posts/{postId}/comments")
    public ApiResponse<Page<CommentResponse>> getComments(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(commentService.getComments(postId, page, size, userId));
    }

    // ==================== 点赞 ====================

    @PostMapping("/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal Long userId,
            @RequestParam("targetType") String targetType,
            @RequestParam("targetId") Long targetId) {
        return ApiResponse.success(interactionService.toggleLike(userId, targetType, targetId));
    }

    /** 便捷端点：直接对帖子点赞/取消 */
    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Map<String, Object>> toggleLikePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId) {
        return ApiResponse.success(interactionService.toggleLike(userId, "POST", postId));
    }

    // ==================== 收藏 ====================

    @PostMapping("/posts/{postId}/collect")
    public ApiResponse<Map<String, Object>> toggleCollect(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId) {
        return ApiResponse.success(interactionService.toggleCollect(userId, "POST", postId));
    }

    @GetMapping("/my/collections")
    public ApiResponse<Page<CommunityPostResponse>> getMyCollections(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return ApiResponse.success(interactionService.getUserCollections(userId, page, size));
    }
}
