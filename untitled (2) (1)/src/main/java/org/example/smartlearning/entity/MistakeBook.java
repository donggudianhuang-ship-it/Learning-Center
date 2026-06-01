package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 错题本实体类
 */
@Data
@TableName("mistake_book")
public class MistakeBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private String mistakeType;

    private Integer reviewCount;

    private LocalDateTime lastReviewAt;

    private LocalDate nextReviewDate;

    private Integer masteryLevel;

    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
