package org.example.smartlearning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.CreatePostRequest;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.entity.CommunityPost;
import org.example.smartlearning.entity.PostComment;
import org.example.smartlearning.service.community.CommunityService;
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

    private final CommunityService communityService;

    @PostMapping("/posts")
    public ApiResponse<CommunityPost> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        CommunityPost post = communityService.createPost(
                userId, request.getTitle(), request.getContent(),
                request.getSubjectId(), request.getAnonymous()
        );
        return ApiResponse.success("发布成功", post);
    }

    @GetMapping("/public/posts")
    public ApiResponse<Page<Map<String, Object>>> getPublicPosts(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(communityService.getPublicPosts(subjectId, page, size, userId));
    }

    @GetMapping("/public/posts/{postId}")
    public ApiResponse<Map<String, Object>> getPostDetail(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(communityService.getPostDetail(postId, userId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<PostComment> addComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam("content") String content) {
        return ApiResponse.success(communityService.addComment(userId, postId, parentId, content));
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Map<String, Object>> toggleLikePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId) {
        boolean liked = communityService.toggleLike(userId, postId);
        Map<String, Object> result = Map.of(
            "liked", liked,
            "message", liked ? "点赞成功" : "取消点赞"
        );
        return ApiResponse.success(result);
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId) {
        communityService.deletePost(userId, postId);
        return ApiResponse.success();
    }

    @GetMapping("/my/posts")
    public ApiResponse<List<Map<String, Object>>> getMyPosts(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(communityService.getUserPosts(userId));
    }
}
