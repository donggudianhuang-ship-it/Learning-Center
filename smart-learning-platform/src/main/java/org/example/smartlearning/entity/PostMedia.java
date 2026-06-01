package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_media")
public class PostMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private String mediaType;  // IMAGE or VIDEO

    private String url;

    private String thumbnailUrl;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
