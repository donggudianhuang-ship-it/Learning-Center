package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学习笔记实体类
 */
@Data
@TableName("study_note")
public class StudyNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Long subjectId;

    private Long knowledgeId;

    private Integer isPublic;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
