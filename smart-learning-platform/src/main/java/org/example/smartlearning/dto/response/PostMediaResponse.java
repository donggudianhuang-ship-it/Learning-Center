package org.example.smartlearning.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 帖子媒体文件响应（图片/视频）
 */
@Data
@Builder
public class PostMediaResponse {
    private Long id;
    private String mediaType;  // IMAGE or VIDEO
    private String url;
    private String thumbnailUrl;
    private Integer sortOrder;
}
