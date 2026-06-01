package org.example.smartlearning.service.grading;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.SubmitAnswerRequest;
import org.example.smartlearning.dto.request.SubmitExamRequest;
import org.example.smartlearning.dto.response.ExamGradingResponse;
import org.example.smartlearning.dto.response.ExamPaperResponse;
import org.example.smartlearning.dto.response.GradingResultResponse;
import org.example.smartlearning.entity.*;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.*;
import org.example.smartlearning.service.learning.KnowledgeTaggingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 判卷服务类
 */
@Service
@RequiredArgsConstructor
public class GradingService {

    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final MistakeBookMapper mistakeBookMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final ClaudeService claudeService;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamSubmissionMapper examSubmissionMapper;
    private final SubjectMapper subjectMapper;
    private final KnowledgeTaggingService knowledgeTaggingService;

    /**
     * 提交答案并批改
     */
    @Transactional
    public GradingResultResponse submitAndGrade(Long userId, SubmitAnswerRequest request) {
        Question question = questionMapper.selectById(request.getQuestionId());
        if (question == null) {
            throw BusinessException.of("题目不存在");
        }

        GradingResultResponse result = gradeQuestion(question, request.getUserAnswer());

        // 保存答题记录
        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setQuestionId(question.getId());
        record.setExamSubmissionId(request.getExamSubmissionId());
        record.setUserAnswer(request.getUserAnswer());
        record.setIsCorrect(result.getIsCorrect() ? 1 : 0);
        record.setScore(result.getScore());
        record.setAiAnalysis(result.getAiAnalysis());
        record.setMistakeType(result.getMistakeType());
        answerRecordMapper.insert(record);

        result.setAnswerRecordId(record.getId());

        // 如果答错，加入错题本
        if (!result.getIsCorrect()) {
            addToMistakeBook(userId, question, result.getMistakeType());
        }

        // 更新知识点掌握度
        updateKnowledgeMastery(userId, question, result.getIsCorrect());

        return result;
    }

    /**
     * 判断是否为客观题
     */
    private boolean isObjectiveQuestion(String type) {
        return "SINGLE_CHOICE".equals(type) || "MULTI_CHOICE".equals(type) || "TRUE_FALSE".equals(type);
    }

    private GradingResultResponse gradeQuestion(Question question, String userAnswer) {
        if (isObjectiveQuestion(question.getType())) {
            return claudeService.gradeObjectiveQuestion(question, userAnswer == null ? "" : userAnswer);
        }

        try {
            return claudeService.gradeSubjectiveQuestion(question, userAnswer == null ? "" : userAnswer);
        } catch (Exception ex) {
            return claudeService.strictSubjectiveFallback(question, userAnswer,
                    "AI批改暂不可用，系统已使用严格兜底判分：" + ex.getMessage());
        }
    }

    /**
     * 添加到错题本
     */
    private void addToMistakeBook(Long userId, Question question, String mistakeType) {
        MistakeBook existing = mistakeBookMapper.selectOne(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .eq(MistakeBook::getQuestionId, question.getId())
        );

        if (existing == null) {
            MistakeBook mistake = new MistakeBook();
            mistake.setUserId(userId);
            mistake.setQuestionId(question.getId());
            mistake.setMistakeType(mistakeType);
            mistake.setReviewCount(0);
            mistake.setMasteryLevel(0);
            mistake.setNextReviewDate(LocalDate.now().plusDays(1)); // 明天复习
            mistakeBookMapper.insert(mistake);
        } else {
            existing.setReviewCount(existing.getReviewCount());
            existing.setNextReviewDate(LocalDate.now().plusDays(1));
            mistakeBookMapper.updateById(existing);
        }
    }

    /**
     * 更新知识点掌握度
     */
    private void updateKnowledgeMastery(Long userId, Question question, boolean isCorrect) {
        List<Long> knowledgeIds = knowledgeTaggingService.resolveKnowledgeIds(question);
        if (knowledgeIds.isEmpty()) {
            return;
        }

        for (Long knowledgeId : knowledgeIds) {
            KnowledgeMastery mastery = knowledgeMasteryMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeMastery>()
                            .eq(KnowledgeMastery::getUserId, userId)
                            .eq(KnowledgeMastery::getKnowledgeId, knowledgeId)
            );

            if (mastery == null) {
                mastery = new KnowledgeMastery();
                mastery.setUserId(userId);
                mastery.setKnowledgeId(knowledgeId);
                mastery.setTotalQuestions(1);
                mastery.setCorrectQuestions(isCorrect ? 1 : 0);
                mastery.setMasteryLevel(BigDecimal.valueOf(isCorrect ? 100 : 0));
                mastery.setLastPracticeAt(LocalDateTime.now());
                knowledgeMasteryMapper.insert(mastery);
            } else {
                int total = mastery.getTotalQuestions() + 1;
                int correct = mastery.getCorrectQuestions() + (isCorrect ? 1 : 0);
                mastery.setTotalQuestions(total);
                mastery.setCorrectQuestions(correct);
                mastery.setMasteryLevel(BigDecimal.valueOf((double) correct / total * 100));
                mastery.setLastPracticeAt(LocalDateTime.now());
                knowledgeMasteryMapper.updateById(mastery);
            }
        }
    }

    /**
     * 提交整卷并批改
     */
    @Transactional
    public ExamGradingResponse submitAndGradeExam(Long userId, SubmitExamRequest request) {
        // 获取试卷信息
        ExamPaper examPaper = examPaperMapper.selectById(request.getExamId());
        if (examPaper == null) {
            throw BusinessException.of("试卷不存在");
        }

        // 获取试卷题目
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, request.getExamId())
                        .orderByAsc(ExamQuestion::getSortOrder)
        );

        if (examQuestions.isEmpty()) {
            throw BusinessException.of("试卷没有题目");
        }

        // 解析用户答案
        Map<Long, String> userAnswers = parseAnswers(request.getAnswers());

        // 创建提交记录
        ExamSubmission submission = new ExamSubmission();
        submission.setUserId(userId);
        submission.setExamId(request.getExamId());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(1);
        examSubmissionMapper.insert(submission);

        // 批改各题
        List<ExamGradingResponse.QuestionGradingDetail> questionDetails = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        int correctCount = 0;
        int wrongCount = 0;

        for (ExamQuestion eq : examQuestions) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (question == null) continue;

            String userAnswer = userAnswers.getOrDefault(eq.getQuestionId(), "");
            maxScore = maxScore.add(eq.getScore());

            // 批改
            GradingResultResponse gradingResult = gradeQuestion(question, userAnswer);

            // 调整分数比例
            BigDecimal questionScore = gradingResult.getScore()
                    .multiply(eq.getScore())
                    .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            gradingResult.setScore(questionScore);
            gradingResult.setMaxScore(eq.getScore());

            totalScore = totalScore.add(questionScore);

            if (gradingResult.getIsCorrect()) {
                correctCount++;
            } else {
                wrongCount++;
            }

            // 保存答题记录
            AnswerRecord record = new AnswerRecord();
            record.setUserId(userId);
            record.setQuestionId(question.getId());
            record.setExamSubmissionId(submission.getId());
            record.setUserAnswer(userAnswer);
            record.setIsCorrect(gradingResult.getIsCorrect() ? 1 : 0);
            record.setScore(gradingResult.getScore());
            record.setAiAnalysis(gradingResult.getAiAnalysis());
            record.setMistakeType(gradingResult.getMistakeType());
            answerRecordMapper.insert(record);

            // 构建详情
            ExamGradingResponse.QuestionGradingDetail detail = new ExamGradingResponse.QuestionGradingDetail();
            detail.setQuestionId(question.getId());
            detail.setQuestionOrder(eq.getSortOrder());
            detail.setQuestionContent(question.getContent());
            detail.setQuestionType(question.getType());
            detail.setDifficulty(question.getDifficulty());
            detail.setUserAnswer(userAnswer);
            detail.setCorrectAnswer(question.getAnswer());
            detail.setIsCorrect(gradingResult.getIsCorrect());
            detail.setScore(gradingResult.getScore());
            detail.setMaxScore(eq.getScore());
            detail.setAiAnalysis(gradingResult.getAiAnalysis());
            detail.setMistakeType(gradingResult.getMistakeType());
            detail.setKnowledgePoints(getKnowledgePointNames(question.getKnowledgeIds()));

            questionDetails.add(detail);

            // 答错加入错题本
            if (!gradingResult.getIsCorrect()) {
                addToMistakeBook(userId, question, gradingResult.getMistakeType());
            }

            // 更新知识点掌握度
            updateKnowledgeMastery(userId, question, gradingResult.getIsCorrect());
        }

        // 更新提交记录
        submission.setTotalScore(totalScore);
        examSubmissionMapper.updateById(submission);

        // 生成错误点分析
        List<ExamGradingResponse.ErrorPointAnalysis> errorPoints = analyzeErrorPoints(questionDetails);

        // 生成AI总体评价
        String aiSummary = generateAiSummary(examPaper, questionDetails, totalScore, maxScore);

        // 构建响应
        ExamGradingResponse response = new ExamGradingResponse();
        response.setSubmissionId(submission.getId());
        response.setExamId(examPaper.getId());
        response.setExamName(examPaper.getName());
        response.setTotalScore(totalScore);
        response.setMaxScore(maxScore);
        response.setAccuracyRate(maxScore.compareTo(BigDecimal.ZERO) > 0
                ? totalScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        response.setTotalQuestions(examQuestions.size());
        response.setCorrectCount(correctCount);
        response.setWrongCount(wrongCount);
        response.setDuration(request.getDuration());
        response.setSubmittedAt(submission.getSubmittedAt());
        response.setQuestions(questionDetails);
        response.setErrorPoints(errorPoints);
        response.setAiSummary(aiSummary);

        return response;
    }

    /**
     * 解析答案字符串
     */
    private Map<Long, String> parseAnswers(String answers) {
        Map<Long, String> result = new HashMap<>();
        if (answers == null || answers.isEmpty()) {
            return result;
        }

        String[] parts = answers.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                try {
                    Long questionId = Long.parseLong(kv[0].trim());
                    String answer = kv[1].trim();
                    result.put(questionId, answer);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    /**
     * 获取知识点名称
     */
    private String getKnowledgePointNames(String knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return "";
        }

        StringBuilder names = new StringBuilder();
        String[] ids = knowledgeIds.split(",");
        for (String idStr : ids) {
            try {
                Long id = Long.parseLong(idStr.trim());
                KnowledgePoint kp = knowledgePointMapper.selectById(id);
                if (kp != null) {
                    if (names.length() > 0) names.append(", ");
                    names.append(kp.getName());
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return names.toString();
    }

    /**
     * 分析错误点
     */
    private List<ExamGradingResponse.ErrorPointAnalysis> analyzeErrorPoints(
            List<ExamGradingResponse.QuestionGradingDetail> details) {

        return details.stream()
                .filter(d -> !d.getIsCorrect())
                .map(d -> {
                    ExamGradingResponse.ErrorPointAnalysis analysis = new ExamGradingResponse.ErrorPointAnalysis();
                    analysis.setQuestionId(d.getQuestionId());
                    analysis.setQuestionOrder(d.getQuestionOrder());
                    analysis.setQuestionContent(d.getQuestionContent());
                    analysis.setUserAnswer(d.getUserAnswer());
                    analysis.setCorrectAnswer(d.getCorrectAnswer());
                    analysis.setMistakeType(d.getMistakeType());
                    analysis.setMistakeReason(generateMistakeReason(d));
                    analysis.setKnowledgeGap(d.getKnowledgePoints());
                    analysis.setSuggestion(generateSuggestion(d));
                    return analysis;
                })
                .collect(Collectors.toList());
    }

    /**
     * 生成错误原因
     */
    private String generateMistakeReason(ExamGradingResponse.QuestionGradingDetail detail) {
        String mistakeType = detail.getMistakeType();
        if (mistakeType == null) {
            mistakeType = "UNKNOWN";
        }

        return switch (mistakeType) {
            case "CONCEPT_ERROR" -> "概念理解错误，需要重新学习相关知识点";
            case "CARELESS" -> "粗心大意，需要更加仔细审题";
            case "WRONG_APPROACH" -> "解题思路错误，需要掌握正确的解题方法";
            case "INCOMPLETE" -> "答案不完整，遗漏了关键要点";
            case "KNOWLEDGE_GAP" -> "知识点掌握不牢固，需要加强练习";
            default -> "答案有误，请参考解析进行复习";
        };
    }

    /**
     * 生成改进建议
     */
    private String generateSuggestion(ExamGradingResponse.QuestionGradingDetail detail) {
        StringBuilder suggestion = new StringBuilder();

        if (detail.getKnowledgePoints() != null && !detail.getKnowledgePoints().isEmpty()) {
            suggestion.append("建议复习知识点：").append(detail.getKnowledgePoints()).append("。");
        }

        String type = detail.getQuestionType();
        if ("SINGLE_CHOICE".equals(type) || "MULTI_CHOICE".equals(type)) {
            suggestion.append("注意审题，排除干扰选项。");
        } else if ("TRUE_FALSE".equals(type)) {
            suggestion.append("仔细分析题目中的关键词。");
        } else if ("FILL_BLANK".equals(type)) {
            suggestion.append("注意填空的完整性和准确性。");
        } else {
            suggestion.append("注意答题的完整性和逻辑性。");
        }

        return suggestion.toString();
    }

    /**
     * 生成AI总体评价
     */
    private String generateAiSummary(ExamPaper examPaper,
                                     List<ExamGradingResponse.QuestionGradingDetail> details,
                                     BigDecimal totalScore, BigDecimal maxScore) {

        long correctCount = details.stream().filter(ExamGradingResponse.QuestionGradingDetail::getIsCorrect).count();
        long wrongCount = details.size() - correctCount;
        double accuracy = maxScore.compareTo(BigDecimal.ZERO) > 0
                ? totalScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP).doubleValue()
                : 0;

        // 统计错误类型
        Map<String, Long> mistakeTypes = details.stream()
                .filter(d -> !d.getIsCorrect())
                .collect(Collectors.groupingBy(
                        d -> d.getMistakeType() != null ? d.getMistakeType() : "UNKNOWN",
                        Collectors.counting()
                ));

        // 统计薄弱知识点
        Set<String> weakKnowledge = details.stream()
                .filter(d -> !d.getIsCorrect())
                .map(ExamGradingResponse.QuestionGradingDetail::getKnowledgePoints)
                .filter(kp -> kp != null && !kp.isEmpty())
                .collect(Collectors.toSet());

        StringBuilder summary = new StringBuilder();
        summary.append("## 试卷批改报告\n\n");
        summary.append("### 整体表现\n");
        summary.append(String.format("- 总分：%.1f / %.1f 分\n", totalScore, maxScore));
        summary.append(String.format("- 正确率：%.1f%%\n", accuracy));
        summary.append(String.format("- 正确题数：%d / %d\n", correctCount, details.size()));

        if (!mistakeTypes.isEmpty()) {
            summary.append("\n### 错误类型分析\n");
            mistakeTypes.forEach((type, count) -> {
                String typeName = switch (type) {
                    case "CONCEPT_ERROR" -> "概念理解错误";
                    case "CARELESS" -> "粗心大意";
                    case "WRONG_APPROACH" -> "解题思路错误";
                    case "INCOMPLETE" -> "答案不完整";
                    case "KNOWLEDGE_GAP" -> "知识点缺失";
                    default -> "其他";
                };
                summary.append(String.format("- %s：%d 题\n", typeName, count));
            });
        }

        if (!weakKnowledge.isEmpty()) {
            summary.append("\n### 薄弱知识点\n");
            weakKnowledge.forEach(kp -> summary.append("- ").append(kp).append("\n"));
        }

        summary.append("\n### 学习建议\n");
        if (accuracy >= 80) {
            summary.append("表现优秀！继续保持，可以尝试挑战更高难度的题目。");
        } else if (accuracy >= 60) {
            summary.append("基础掌握良好，建议针对薄弱知识点进行专项练习。");
        } else {
            summary.append("需要加强基础知识的复习，建议重新学习相关知识点后再进行练习。");
        }

        return summary.toString();
    }

    /**
     * 获取试卷详情
     */
    public ExamPaperResponse getExamPaper(Long examId) {
        ExamPaper examPaper = examPaperMapper.selectById(examId);
        if (examPaper == null) {
            throw BusinessException.of("试卷不存在");
        }

        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSortOrder)
        );

        ExamPaperResponse response = new ExamPaperResponse();
        response.setId(examPaper.getId());
        response.setName(examPaper.getName());
        response.setSubjectId(examPaper.getSubjectId());
        response.setDescription(examPaper.getDescription());
        response.setDuration(examPaper.getDuration());
        response.setTotalQuestions(examQuestions.size());
        response.setTotalScore(examQuestions.stream()
                .mapToInt(eq -> eq.getScore().intValue())
                .sum());
        response.setStatus(examPaper.getStatus());

        // 获取科目名称
        Subject subject = subjectMapper.selectById(examPaper.getSubjectId());
        response.setSubjectName(subject != null ? subject.getName() : "");

        // 构建题目列表（不包含答案）
        List<ExamPaperResponse.QuestionDetail> questions = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (question == null) continue;

            ExamPaperResponse.QuestionDetail detail = new ExamPaperResponse.QuestionDetail();
            detail.setQuestionId(question.getId());
            detail.setSortOrder(eq.getSortOrder());
            detail.setScore(eq.getScore().intValue());
            detail.setContent(question.getContent());
            detail.setType(question.getType());
            detail.setDifficulty(question.getDifficulty());
            detail.setOptions(question.getOptions());
            detail.setKnowledgePoints(getKnowledgePointNames(question.getKnowledgeIds()));

            questions.add(detail);
        }
        response.setQuestions(questions);

        return response;
    }

    /**
     * 获取可用试卷列表
     */
    public List<ExamPaperResponse> getAvailableExams(Long subjectId) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getStatus, 1);
        if (subjectId != null) {
            wrapper.eq(ExamPaper::getSubjectId, subjectId);
        }

        List<ExamPaper> papers = examPaperMapper.selectList(wrapper);

        return papers.stream().map(paper -> {
            ExamPaperResponse response = new ExamPaperResponse();
            response.setId(paper.getId());
            response.setName(paper.getName());
            response.setSubjectId(paper.getSubjectId());
            response.setDescription(paper.getDescription());
            response.setDuration(paper.getDuration());
            response.setStatus(paper.getStatus());

            // 获取科目名称
            Subject subject = subjectMapper.selectById(paper.getSubjectId());
            response.setSubjectName(subject != null ? subject.getName() : "");

            // 获取题目数量和总分
            Long questionCount = examQuestionMapper.selectCount(
                    new LambdaQueryWrapper<ExamQuestion>()
                            .eq(ExamQuestion::getExamId, paper.getId())
            );
            response.setTotalQuestions(questionCount.intValue());

            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 获取批改历史
     */
    public List<ExamGradingResponse> getGradingHistory(Long userId, Integer limit) {
        List<ExamSubmission> submissions = examSubmissionMapper.selectList(
                new LambdaQueryWrapper<ExamSubmission>()
                        .eq(ExamSubmission::getUserId, userId)
                        .orderByDesc(ExamSubmission::getSubmittedAt)
                        .last("LIMIT " + limit)
        );

        return submissions.stream().map(submission -> {
            ExamGradingResponse response = new ExamGradingResponse();
            response.setSubmissionId(submission.getId());
            response.setExamId(submission.getExamId());
            response.setTotalScore(submission.getTotalScore());
            response.setSubmittedAt(submission.getSubmittedAt());

            ExamPaper paper = examPaperMapper.selectById(submission.getExamId());
            if (paper != null) {
                response.setExamName(paper.getName());
            }

            List<AnswerRecord> records = answerRecordMapper.selectList(
                    new LambdaQueryWrapper<AnswerRecord>()
                            .eq(AnswerRecord::getExamSubmissionId, submission.getId())
            );
            BigDecimal maxScore = calculateSubmissionMaxScore(submission.getExamId(), records);
            int correctCount = (int) records.stream()
                    .filter(record -> record.getIsCorrect() != null && record.getIsCorrect() == 1)
                    .count();

            response.setMaxScore(maxScore);
            response.setTotalQuestions(records.size());
            response.setCorrectCount(correctCount);
            response.setWrongCount(records.size() - correctCount);
            response.setAccuracyRate(maxScore.compareTo(BigDecimal.ZERO) > 0 && submission.getTotalScore() != null
                    ? submission.getTotalScore().multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 获取提交详情
     */
    public ExamGradingResponse getSubmissionDetail(Long userId, Long submissionId) {
        ExamSubmission submission = examSubmissionMapper.selectById(submissionId);
        if (submission == null || !submission.getUserId().equals(userId)) {
            throw BusinessException.of("提交记录不存在");
        }

        // 获取答题记录
        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getExamSubmissionId, submissionId)
        );

        ExamPaper examPaper = examPaperMapper.selectById(submission.getExamId());

        ExamGradingResponse response = new ExamGradingResponse();
        response.setSubmissionId(submission.getId());
        response.setExamId(submission.getExamId());
        response.setExamName(examPaper != null ? examPaper.getName() : "");
        BigDecimal totalScore = submission.getTotalScore() == null ? BigDecimal.ZERO : submission.getTotalScore();
        response.setTotalScore(totalScore);
        response.setSubmittedAt(submission.getSubmittedAt());

        // 构建题目详情
        List<ExamGradingResponse.QuestionGradingDetail> questions = new ArrayList<>();
        BigDecimal maxScore = BigDecimal.ZERO;
        int correctCount = 0;

        for (AnswerRecord record : records) {
            Question question = questionMapper.selectById(record.getQuestionId());
            if (question == null) continue;

            // 获取题目分数
            ExamQuestion eq = examQuestionMapper.selectOne(
                    new LambdaQueryWrapper<ExamQuestion>()
                            .eq(ExamQuestion::getExamId, submission.getExamId())
                            .eq(ExamQuestion::getQuestionId, record.getQuestionId())
            );
            BigDecimal questionMaxScore = eq != null ? eq.getScore() : BigDecimal.TEN;
            maxScore = maxScore.add(questionMaxScore);

            if (record.getIsCorrect() == 1) {
                correctCount++;
            }

            ExamGradingResponse.QuestionGradingDetail detail = new ExamGradingResponse.QuestionGradingDetail();
            detail.setQuestionId(record.getQuestionId());
            detail.setQuestionOrder(eq != null ? eq.getSortOrder() : questions.size() + 1);
            detail.setQuestionContent(question.getContent());
            detail.setQuestionType(question.getType());
            detail.setDifficulty(question.getDifficulty());
            detail.setUserAnswer(record.getUserAnswer());
            detail.setCorrectAnswer(question.getAnswer());
            detail.setIsCorrect(record.getIsCorrect() == 1);
            detail.setScore(record.getScore());
            detail.setMaxScore(questionMaxScore);
            detail.setAiAnalysis(record.getAiAnalysis());
            detail.setMistakeType(record.getMistakeType());
            detail.setKnowledgePoints(getKnowledgePointNames(question.getKnowledgeIds()));

            questions.add(detail);
        }

        response.setQuestions(questions);
        response.setMaxScore(maxScore);
        response.setTotalQuestions(records.size());
        response.setCorrectCount(correctCount);
        response.setWrongCount(records.size() - correctCount);
        response.setAccuracyRate(maxScore.compareTo(BigDecimal.ZERO) > 0
                ? totalScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 生成错误点分析
        response.setErrorPoints(analyzeErrorPoints(questions));
        response.setAiSummary(examPaper != null
                ? generateAiSummary(examPaper, questions, totalScore, maxScore)
                : "");

        return response;
    }

    private BigDecimal calculateSubmissionMaxScore(Long examId, List<AnswerRecord> records) {
        BigDecimal maxScore = BigDecimal.ZERO;
        for (AnswerRecord record : records) {
            ExamQuestion eq = examQuestionMapper.selectOne(
                    new LambdaQueryWrapper<ExamQuestion>()
                            .eq(ExamQuestion::getExamId, examId)
                            .eq(ExamQuestion::getQuestionId, record.getQuestionId())
            );
            maxScore = maxScore.add(eq != null ? eq.getScore() : BigDecimal.TEN);
        }
        return maxScore;
    }
}
