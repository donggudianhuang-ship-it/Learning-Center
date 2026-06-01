package org.example.smartlearning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.smartlearning.dto.request.CreatePostRequest;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.dto.response.CommentResponse;
import org.example.smartlearning.dto.response.CommunityPostResponse;
import org.example.smartlearning.dto.response.PostDetailResponse;
import org.example.smartlearning.dto.response.PostMediaResponse;
import org.example.smartlearning.entity.CommunitySection;
import org.example.smartlearning.mapper.CommunitySectionMapper;
import org.example.smartlearning.service.community.CommentService;
import org.example.smartlearning.service.community.InteractionService;
import org.example.smartlearning.service.community.MediaService;
import org.example.smartlearning.service.community.PostService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final MediaService mediaService;
    private final CommunitySectionMapper sectionMapper;

    // ==================== 分区 ====================

    @GetMapping("/sections")
    public ApiResponse<List<CommunitySection>> getSections() {
        return ApiResponse.success(sectionMapper.selectList(
                new LambdaQueryWrapper<CommunitySection>().orderByAsc(CommunitySection::getSortOrder)));
    }

    // ==================== 帖子 ====================

    @PostMapping("/posts")
    public ApiResponse<CommunityPostResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success("发布成功",
                postService.createPost(userId, request.getTitle(), request.getContent(),
                        request.getSubjectId(), request.getAnonymous(), request.getMediaUrls(),
                        request.getSectionId()));
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
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(postService.getPublicPosts(subjectId, page, size, sort, userId, keyword, sectionId));
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

    // ==================== 媒体上传 ====================

    /**
     * 向已有帖子追加图片/视频
     */
    @PostMapping("/posts/{postId}/media")
    public ApiResponse<List<PostMediaResponse>> uploadPostMedia(
            @AuthenticationPrincipal Long userId,
            @PathVariable("postId") Long postId,
            @RequestParam("files") List<MultipartFile> files) {
        List<PostMediaResponse> results = mediaService.attachMediaToPost(postId, files).stream()
                .map(mediaService::toResponse).collect(Collectors.toList());
        return ApiResponse.success("上传成功", results);
    }

    /**
     * 预上传媒体文件（发帖前先传图，返回URL，发帖时通过 mediaUrls 关联）
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> preUploadMedia(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        String url;
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video/")) {
            url = mediaService.preUploadVideo(file);
        } else {
            url = mediaService.preUploadImage(file);
        }
        return ApiResponse.success(Map.of("url", url, "mediaType",
                contentType != null && contentType.startsWith("video/") ? "VIDEO" : "IMAGE"));
    }
}
