package org.example.smartlearning.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * AI判卷结果响应DTO
 */
@Data
public class GradingResultResponse {

    private Long answerRecordId;
    private Long questionId;
    private Boolean isCorrect;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String aiAnalysis;
    private String mistakeType;
    private String correctAnswer;
    private String analysis;
}
