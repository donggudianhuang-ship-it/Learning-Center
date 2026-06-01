package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点掌握度实体类
 */
@Data
@TableName("knowledge_mastery")
public class KnowledgeMastery {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long knowledgeId;

    private BigDecimal masteryLevel;

    private Integer totalQuestions;

    private Integer correctQuestions;

    private LocalDateTime lastPracticeAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
