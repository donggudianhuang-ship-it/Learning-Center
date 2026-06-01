package org.example.smartlearning.service.common;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.config.MinioConfig;
import org.example.smartlearning.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 通用文件存储服务 —— MinIO 对象存储
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;   // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;  // 100MB

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public String uploadPostImage(MultipartFile file) {
        validateFile(file, true);
        return upload(file, "community/posts/images/");
    }

    public String uploadPostVideo(MultipartFile file) {
        validateFile(file, false);
        return upload(file, "community/posts/videos/");
    }

    public void delete(String url) {
        if (url == null) return;
        try {
            String path = url.replace(minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/", "");
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(path)
                    .build());
        } catch (Exception ignored) {
        }
    }

    // ── 内部 ──

    private String upload(MultipartFile file, String prefix) {
        ensureBucketExists();
        String ext = getExtension(file.getOriginalFilename());
        String objectName = prefix + UUID.randomUUID() + ext;
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw BusinessException.of("文件上传失败: " + e.getMessage());
        }
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + objectName;
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucketName()).build());
            }
        } catch (Exception e) {
            throw BusinessException.of("存储服务初始化失败: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file, boolean isImage) {
        if (file.isEmpty()) throw BusinessException.of("文件为空");
        String contentType = file.getContentType();
        if (isImage) {
            if (contentType == null || !contentType.startsWith("image/"))
                throw BusinessException.of("仅支持上传图片 (jpg, png, gif, webp)");
            if (file.getSize() > MAX_IMAGE_SIZE)
                throw BusinessException.of("图片大小不能超过 10MB");
        } else {
            if (contentType == null || !contentType.startsWith("video/"))
                throw BusinessException.of("仅支持上传视频 (mp4, webm, mov)");
            if (file.getSize() > MAX_VIDEO_SIZE)
                throw BusinessException.of("视频大小不能超过 100MB");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
