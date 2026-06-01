package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷提交实体类
 */
@Data
@TableName("exam_submission")
public class ExamSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long examId;

    private BigDecimal totalScore;

    private String aiReport;

    private LocalDateTime submittedAt;

    private Integer status;
}
