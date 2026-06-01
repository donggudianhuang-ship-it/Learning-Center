package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 答题记录实体类
 */
@Data
@TableName("answer_record")
public class AnswerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private Long examSubmissionId;

    private String userAnswer;

    private Integer isCorrect;

    private BigDecimal score;

    private String aiAnalysis;

    private String mistakeType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
