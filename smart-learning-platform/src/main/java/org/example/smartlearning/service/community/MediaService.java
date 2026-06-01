package org.example.smartlearning.service.community;

import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.PostMediaResponse;
import org.example.smartlearning.entity.PostMedia;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.PostMediaMapper;
import org.example.smartlearning.service.common.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子媒体管理 —— 图片/视频的上传、查询、删除
 */
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final int MAX_MEDIA_PER_POST = 9;

    private final PostMediaMapper postMediaMapper;
    private final FileStorageService fileStorageService;

    /**
     * 上传单张图片，返回保存后的媒体记录
     */
    public PostMedia uploadImage(Long postId, int sortOrder, MultipartFile file) {
        String url = fileStorageService.uploadPostImage(file);
        PostMedia media = new PostMedia();
        media.setPostId(postId);
        media.setMediaType("IMAGE");
        media.setUrl(url);
        media.setThumbnailUrl(url); // MinIO 无缩略图时直接用原图
        media.setSortOrder(sortOrder);
        postMediaMapper.insert(media);
        return media;
    }

    /**
     * 上传单个视频
     */
    public PostMedia uploadVideo(Long postId, int sortOrder, MultipartFile file) {
        String url = fileStorageService.uploadPostVideo(file);
        PostMedia media = new PostMedia();
        media.setPostId(postId);
        media.setMediaType("VIDEO");
        media.setUrl(url);
        media.setSortOrder(sortOrder);
        postMediaMapper.insert(media);
        return media;
    }

    /**
     * 批量处理上传文件列表，校验数量限制，返回媒体记录
     */
    @Transactional
    public List<PostMedia> attachMediaToPost(Long postId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return Collections.emptyList();

        // 检查现有媒体数量
        int existing = postMediaMapper.findByPostId(postId).size();
        if (existing + files.size() > MAX_MEDIA_PER_POST) {
            throw BusinessException.of("每个帖子最多上传 " + MAX_MEDIA_PER_POST + " 个附件，当前已有 " + existing + " 个");
        }

        List<PostMedia> results = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("video/")) {
                results.add(uploadVideo(postId, existing + i, file));
            } else {
                results.add(uploadImage(postId, existing + i, file));
            }
        }
        return results;
    }

    /**
     * 删除帖子的所有媒体文件（MinIO文件 + DB记录）
     */
    public void deletePostMedia(Long postId) {
        List<PostMedia> list = postMediaMapper.findByPostId(postId);
        for (PostMedia m : list) {
            fileStorageService.delete(m.getUrl());
        }
        // DB 记录由 ON DELETE CASCADE 自动清理，这里手动清理以防万一
        postMediaMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PostMedia>()
                .eq(PostMedia::getPostId, postId));
    }

    /**
     * 单帖媒体列表
     */
    public List<PostMedia> getPostMedia(Long postId) {
        return postMediaMapper.findByPostId(postId);
    }

    /**
     * 批量加载多个帖子的媒体，key = postId
     */
    public Map<Long, List<PostMediaResponse>> batchLoadMedia(List<Long> postIds) {
        if (postIds.isEmpty()) return Collections.emptyMap();
        List<PostMedia> all = postMediaMapper.findByPostIds(postIds);
        return all.stream()
                .collect(Collectors.groupingBy(PostMedia::getPostId,
                        Collectors.mapping(this::toResponse, Collectors.toList())));
    }

    /**
     * 预上传图片（不关联帖子，仅返回URL，用于发帖前预览）
     */
    public String preUploadImage(MultipartFile file) {
        return fileStorageService.uploadPostImage(file);
    }

    /**
     * 预上传视频（不关联帖子，仅返回URL）
     */
    public String preUploadVideo(MultipartFile file) {
        return fileStorageService.uploadPostVideo(file);
    }

    /**
     * 将预上传的URL列表绑定到帖子（创建PostMedia记录）
     */
    @Transactional
    public void bindMediaUrls(Long postId, List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            PostMedia media = new PostMedia();
            media.setPostId(postId);
            media.setMediaType(url.contains("/videos/") ? "VIDEO" : "IMAGE");
            media.setUrl(url);
            media.setThumbnailUrl(url.contains("/videos/") ? null : url);
            media.setSortOrder(i);
            postMediaMapper.insert(media);
        }
    }

    // ── DTO ──

    public PostMediaResponse toResponse(PostMedia m) {
        return PostMediaResponse.builder()
                .id(m.getId()).mediaType(m.getMediaType())
                .url(m.getUrl()).thumbnailUrl(m.getThumbnailUrl())
                .sortOrder(m.getSortOrder()).build();
    }
}
