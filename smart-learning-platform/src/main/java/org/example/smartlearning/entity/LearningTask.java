package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个性化学习路径任务
 */
@Data
@TableName("learning_task")
public class LearningTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String taskKey;

    private String taskType;

    private String title;

    private String description;

    private Long knowledgeId;

    private String knowledgeName;

    private String questionIds;

    private String actionRoute;

    private Integer priority;

    private Integer estimatedMinutes;

    private BigDecimal progress;

    private BigDecimal targetProgress;

    private String status;

    private LocalDate deadline;

    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
