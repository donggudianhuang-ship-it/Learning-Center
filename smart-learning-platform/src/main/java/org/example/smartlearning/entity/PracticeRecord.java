package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专项练习记录实体类
 */
@Data
@TableName("practice_record")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 练习ID（UUID）
     */
    private String practiceId;

    /**
     * 练习类型: SUBJECT, KNOWLEDGE, TYPE, MISTAKE
     */
    private String practiceType;

    /**
     * 科目ID
     */
    private Long subjectId;

    /**
     * 知识点ID
     */
    private Long knowledgeId;

    /**
     * 题型
     */
    private String questionType;

    /**
     * 难度
     */
    private Integer difficulty;

    /**
     * 总题数
     */
    private Integer totalQuestions;

    /**
     * 正确数
     */
    private Integer correctCount;

    /**
     * 正确率
     */
    private BigDecimal accuracyRate;

    /**
     * 总得分
     */
    private BigDecimal totalScore;

    /**
     * 用时（秒）
     */
    private Integer duration;

    /**
     * 状态: 0-进行中, 1-已完成
     */
    private Integer status;

    /**
     * 开始时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime endTime;
}
