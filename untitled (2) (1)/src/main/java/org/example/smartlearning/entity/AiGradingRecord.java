package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍照/上传图片 AI 判卷记录
 */
@Data
@TableName("ai_grading_record")
public class AiGradingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String subject;

    private String grade;

    private BigDecimal totalScore;

    private BigDecimal maxScore;

    private BigDecimal accuracyRate;

    private Integer correctCount;

    private Integer wrongCount;

    private String resultJson;

    private String originalFileName;

    private LocalDateTime createdAt;
}
