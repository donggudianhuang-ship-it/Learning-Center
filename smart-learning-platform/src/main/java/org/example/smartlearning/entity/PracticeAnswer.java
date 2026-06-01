package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专项练习答题记录实体类
 */
@Data
@TableName("practice_answer")
public class PracticeAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 练习记录ID
     */
    private Long practiceRecordId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目顺序
     */
    private Integer questionOrder;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 是否正确
     */
    private Integer isCorrect;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 错误类型
     */
    private String mistakeType;

    /**
     * AI分析
     */
    private String aiAnalysis;

    /**
     * 答题时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime answerTime;
}