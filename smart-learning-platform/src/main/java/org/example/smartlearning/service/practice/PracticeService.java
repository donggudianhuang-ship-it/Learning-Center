package org.example.smartlearning.service.practice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.PracticeRequest;
import org.example.smartlearning.dto.response.GradingResultResponse;
import org.example.smartlearning.dto.response.PracticeResponse;
import org.example.smartlearning.dto.response.PracticeResultResponse;
import org.example.smartlearning.entity.*;
import org.example.smartlearning.mapper.*;
import org.example.smartlearning.service.grading.ClaudeService;
import org.example.smartlearning.service.learning.KnowledgeTaggingService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 专项练习服务类
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final MistakeBookMapper mistakeBookMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeAnswerMapper practiceAnswerMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KnowledgeTaggingService knowledgeTaggingService;
    private final ClaudeService claudeService;

    private static final String PRACTICE_CACHE_PREFIX = "practice:";
    private static final long PRACTICE_EXPIRE_HOURS = 2;

    /**
     * 获取专项练习类型列表
     */
    public List<Map<String, Object>> getPracticeTypes() {
        List<Map<String, Object>> types = new ArrayList<>();

        // 按科目练习
        List<Subject> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getSortOrder)
        );
        // 按名称去重，保留第一个
        Map<String, Subject> uniqueSubjects = new LinkedHashMap<>();
        for (Subject s : subjects) {
            uniqueSubjects.putIfAbsent(s.getName(), s);
        }
        Map<String, Object> subjectType = new HashMap<>();
        subjectType.put("type", "SUBJECT");
        subjectType.put("name", "按科目练习");
        subjectType.put("description", "选择科目进行专项练习");
        subjectType.put("icon", "📚");
        subjectType.put("items", uniqueSubjects.values().stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("name", s.getName());
            item.put("icon", s.getIcon());
            return item;
        }).collect(Collectors.toList()));
        types.add(subjectType);

        // 按题型练习
        Map<String, Object> questionType = new HashMap<>();
        questionType.put("type", "TYPE");
        questionType.put("name", "按题型练习");
        questionType.put("description", "针对特定题型进行强化训练");
        questionType.put("icon", "✏️");
        questionType.put("items", Arrays.asList(
                Map.of("id", "SINGLE_CHOICE", "name", "单选题"),
                Map.of("id", "MULTI_CHOICE", "name", "多选题"),
                Map.of("id", "TRUE_FALSE", "name", "判断题"),
                Map.of("id", "FILL_BLANK", "name", "填空题"),
                Map.of("id", "SHORT_ANSWER", "name", "简答题"),
                Map.of("id", "ESSAY", "name", "论述题")
        ));
        types.add(questionType);

        // 错题专项
        Map<String, Object> mistakeType = new HashMap<>();
        mistakeType.put("type", "MISTAKE");
        mistakeType.put("name", "错题专项");
        mistakeType.put("description", "针对错题进行强化训练");
        mistakeType.put("icon", "❌");
        types.add(mistakeType);

        return types;
    }

    /**
     * 获取科目的知识点列表（用于知识点专项练习）
     */
    public List<Map<String, Object>> getKnowledgePointsBySubject(Long subjectId) {
        List<KnowledgePoint> points = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getSubjectId, subjectId)
                        .orderByAsc(KnowledgePoint::getLevel)
        );

        return points.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("level", p.getLevel());
            item.put("parentId", p.getParentId());
            item.put("description", p.getDescription());
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 获取科目题库中实际存在的题型，避免按题型练习出现空入口。
     */
    public List<Map<String, Object>> getQuestionTypesBySubject(Long subjectId) {
        Subject subject = subjectId == null ? null : subjectMapper.selectById(subjectId);
        String subjectName = subject == null ? "" : subject.getName();
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(subjectId != null, Question::getSubjectId, subjectId)
                        .isNotNull(Question::getType)
                        .select(Question::getType)
        );

        Map<String, Long> counts = questions.stream()
                .map(Question::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(type -> type, LinkedHashMap::new, Collectors.counting()));

        List<String> order = Arrays.asList("SINGLE_CHOICE", "MULTI_CHOICE", "FILL_BLANK", "SHORT_ANSWER", "ESSAY", "TRUE_FALSE");
        return order.stream()
                .filter(counts::containsKey)
                .map(type -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", type);
                    item.put("name", getQuestionTypeDisplayName(type, subjectId));
                    item.put("limit", getQuestionTypeLimit(subjectName, type));
                    item.put("count", counts.get(type));
                    item.put("subjectId", subjectId);
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 生成专项练习
     */
    @Transactional
    public PracticeResponse generatePractice(Long userId, PracticeRequest request) {
        if ("DIFFICULTY".equals(request.getPracticeType())) {
            throw new RuntimeException("按难度练习已移除，请选择知识点、题型或错题专项练习");
        }

        String practiceId = UUID.randomUUID().toString().replace("-", "");

        // 查询符合条件的题目
        List<Question> questions = selectQuestions(userId, request);

        if (questions.isEmpty()) {
            throw new RuntimeException("没有找到符合条件的题目");
        }

        // 构建响应
        PracticeResponse response = new PracticeResponse();
        response.setPracticeId(practiceId);
        response.setPracticeType(request.getPracticeType());
        response.setPracticeName(generatePracticeName(request));
        response.setTotalQuestions(questions.size());
        response.setCurrentIndex(0);

        // 构建题目列表
        List<PracticeResponse.QuestionItem> questionItems = new ArrayList<>();
        int order = 1;
        for (Question q : questions) {
            PracticeResponse.QuestionItem item = new PracticeResponse.QuestionItem();
            item.setQuestionId(q.getId());
            item.setOrder(order++);
            item.setType(q.getType());
            item.setContent(q.getContent());
            item.setOptions(q.getOptions());
            item.setDifficulty(q.getDifficulty());
            item.setKnowledgeNames(getKnowledgeNames(q.getKnowledgeIds()));
            item.setSubjectName(getSubjectName(q.getSubjectId()));
            questionItems.add(item);
        }
        response.setQuestions(questionItems);

        // 保存练习配置到Redis
        Map<String, Object> config = new HashMap<>();
        config.put("practiceId", practiceId);
        config.put("practiceType", request.getPracticeType());
        config.put("subjectId", request.getSubjectId());
        config.put("knowledgeId", request.getKnowledgeId());
        config.put("questionType", request.getQuestionType());
        config.put("difficulty", request.getDifficulty());
        config.put("questionIds", questions.stream().map(Question::getId).collect(Collectors.toList()));
        config.put("startTime", LocalDateTime.now());
        config.put("userId", userId);

        String cacheKey = PRACTICE_CACHE_PREFIX + practiceId;
        redisTemplate.opsForValue().set(cacheKey, config, PRACTICE_EXPIRE_HOURS, TimeUnit.HOURS);

        // 创建练习记录
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setPracticeId(practiceId);
        record.setPracticeType(request.getPracticeType());
        record.setSubjectId(request.getSubjectId());
        record.setKnowledgeId(request.getKnowledgeId());
        record.setQuestionType(request.getQuestionType());
        record.setDifficulty(request.getDifficulty());
        record.setTotalQuestions(questions.size());
        record.setCorrectCount(0);
        record.setAccuracyRate(BigDecimal.ZERO);
        record.setTotalScore(BigDecimal.ZERO);
        record.setStatus(0); // 进行中
        record.setStartTime(LocalDateTime.now());
        practiceRecordMapper.insert(record);

        return response;
    }

    /**
     * 提交单题答案
     */
    @Transactional
    public Map<String, Object> submitAnswer(Long userId, String practiceId, Long questionId, String userAnswer, Integer questionOrder) {
        // 获取练习配置
        String cacheKey = PRACTICE_CACHE_PREFIX + practiceId;
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);

        if (config == null) {
            throw new RuntimeException("练习已过期，请重新开始");
        }

        // 获取题目
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }

        // 判断答案是否正确。主观题走 DeepSeek，客观题/填空题走本地规则。
        GradingResultResponse gradingResult = gradePracticeAnswer(question, userAnswer);
        boolean isCorrect = Boolean.TRUE.equals(gradingResult.getIsCorrect());
        BigDecimal score = gradingResult.getScore() == null ? BigDecimal.ZERO : gradingResult.getScore();
        String mistakeType = isCorrect ? "CORRECT" :
                (gradingResult.getMistakeType() == null || gradingResult.getMistakeType().isBlank()
                        ? determineMistakeType(question, userAnswer)
                        : gradingResult.getMistakeType());

        // 获取练习记录
        PracticeRecord record = practiceRecordMapper.selectOne(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getPracticeId, practiceId)
                        .eq(PracticeRecord::getUserId, userId)
        );

        if (record == null) {
            throw new RuntimeException("练习记录不存在");
        }

        // 保存答题记录
        PracticeAnswer answer = new PracticeAnswer();
        answer.setPracticeRecordId(record.getId());
        answer.setUserId(userId);
        answer.setQuestionId(questionId);
        answer.setQuestionOrder(questionOrder != null ? questionOrder : getQuestionOrder(config, questionId));
        answer.setUserAnswer(userAnswer);
        answer.setIsCorrect(isCorrect ? 1 : 0);
        answer.setScore(score);
        answer.setAiAnalysis(gradingResult.getAiAnalysis());
        answer.setAnswerTime(LocalDateTime.now());

        if (!isCorrect) {
            answer.setMistakeType(mistakeType);
        }

        practiceAnswerMapper.insert(answer);

        // 更新练习记录
        if (isCorrect) {
            record.setCorrectCount(record.getCorrectCount() + 1);
        }
        record.setTotalScore(record.getTotalScore().add(score));

        long answered = practiceAnswerMapper.selectCount(
                new LambdaQueryWrapper<PracticeAnswer>()
                        .eq(PracticeAnswer::getPracticeRecordId, record.getId())
        );
        if (answered == record.getTotalQuestions()) {
            // 计算正确率
            BigDecimal accuracy = BigDecimal.valueOf(record.getCorrectCount())
                    .divide(BigDecimal.valueOf(record.getTotalQuestions()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            record.setAccuracyRate(accuracy);
        }

        practiceRecordMapper.updateById(record);

        // 同时保存到答题记录表
        AnswerRecord answerRecord = new AnswerRecord();
        answerRecord.setUserId(userId);
        answerRecord.setQuestionId(questionId);
        answerRecord.setUserAnswer(userAnswer);
        answerRecord.setIsCorrect(isCorrect ? 1 : 0);
        answerRecord.setScore(score);
        answerRecord.setAiAnalysis(gradingResult.getAiAnalysis());
        answerRecord.setMistakeType(isCorrect ? null : mistakeType);
        answerRecord.setCreatedAt(LocalDateTime.now());
        answerRecordMapper.insert(answerRecord);

        // 如果答错，加入错题本
        if (!isCorrect) {
            addToMistakeBook(userId, questionId, answer.getMistakeType());
        }

        // 同步更新知识掌握度，保证练习数据能进入学情分析和学习路径
        updateKnowledgeMastery(userId, question, isCorrect);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("score", score);
        result.put("correctAnswer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        result.put("aiAnalysis", gradingResult.getAiAnalysis());
        result.put("mistakeType", answer.getMistakeType());

        return result;
    }

    /**
     * 完成练习
     */
    @Transactional
    public PracticeResultResponse finishPractice(Long userId, String practiceId) {
        // 获取练习记录
        PracticeRecord record = practiceRecordMapper.selectOne(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getPracticeId, practiceId)
                        .eq(PracticeRecord::getUserId, userId)
        );

        if (record == null) {
            throw new RuntimeException("练习记录不存在");
        }

        // 获取答题记录
        List<PracticeAnswer> answers = practiceAnswerMapper.selectList(
                new LambdaQueryWrapper<PracticeAnswer>()
                        .eq(PracticeAnswer::getPracticeRecordId, record.getId())
                        .orderByAsc(PracticeAnswer::getQuestionOrder)
        );

        // 计算统计数据
        int correctCount = (int) answers.stream().filter(a -> a.getIsCorrect() == 1).count();
        int wrongCount = answers.size() - correctCount;

        BigDecimal accuracyRate = BigDecimal.ZERO;
        if (!answers.isEmpty()) {
            accuracyRate = BigDecimal.valueOf(correctCount)
                    .divide(BigDecimal.valueOf(answers.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 计算用时
        Integer duration = 0;
        if (record.getStartTime() != null) {
            LocalDateTime endTime = LocalDateTime.now();
            duration = (int) java.time.Duration.between(record.getStartTime(), endTime).getSeconds();
        }

        // 更新练习记录
        record.setCorrectCount(correctCount);
        record.setAccuracyRate(accuracyRate);
        record.setDuration(duration);
        record.setStatus(1); // 已完成
        record.setEndTime(LocalDateTime.now());
        practiceRecordMapper.updateById(record);

        // 清除Redis缓存
        String cacheKey = PRACTICE_CACHE_PREFIX + practiceId;
        redisTemplate.delete(cacheKey);

        // 构建响应
        PracticeResultResponse response = new PracticeResultResponse();
        response.setPracticeId(practiceId);
        response.setPracticeType(record.getPracticeType());
        response.setTotalQuestions(record.getTotalQuestions());
        response.setAnsweredQuestions(answers.size());
        response.setCorrectCount(correctCount);
        response.setWrongCount(wrongCount);
        response.setAccuracyRate(accuracyRate);
        response.setTotalScore(record.getTotalScore());
        response.setMaxScore(BigDecimal.valueOf(record.getTotalQuestions() * 10));
        response.setDuration(duration);
        response.setCompletedAt(LocalDateTime.now());

        // 构建各题结果
        List<PracticeResultResponse.QuestionResult> questionResults = new ArrayList<>();
        for (PracticeAnswer answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question != null) {
                PracticeResultResponse.QuestionResult qr = new PracticeResultResponse.QuestionResult();
                qr.setQuestionId(answer.getQuestionId());
                qr.setOrder(answer.getQuestionOrder());
                qr.setQuestionContent(question.getContent());
                qr.setUserAnswer(answer.getUserAnswer());
                qr.setCorrectAnswer(question.getAnswer());
                qr.setIsCorrect(answer.getIsCorrect() == 1);
                qr.setScore(answer.getScore());
                qr.setMaxScore(BigDecimal.TEN);
                qr.setMistakeType(answer.getMistakeType());
                qr.setAiAnalysis(answer.getAiAnalysis());
                questionResults.add(qr);
            }
        }
        response.setQuestionResults(questionResults);

        // 按学科分类答题详情
        response.setSubjectResults(buildSubjectResults(answers));

        // 统计错误知识点
        response.setErrorKnowledgeStats(getErrorKnowledgeStats(answers));

        // 统计错误类型
        response.setErrorTypeStats(getErrorTypeStats(answers));

        // 生成AI建议
        response.setAiSuggestion(generateAiSuggestion(record, answers));

        return response;
    }

    /**
     * 获取练习历史
     */
    public List<Map<String, Object>> getPracticeHistory(Long userId, String practiceType, Integer limit) {
        LambdaQueryWrapper<PracticeRecord> wrapper = new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getStatus, 1)
                .orderByDesc(PracticeRecord::getEndTime);

        if (practiceType != null && !practiceType.isEmpty()) {
            wrapper.eq(PracticeRecord::getPracticeType, practiceType);
        }

        if (limit != null) {
            wrapper.last("LIMIT " + limit);
        }

        List<PracticeRecord> records = practiceRecordMapper.selectList(wrapper);

        return records.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("practiceId", r.getPracticeId());
            item.put("practiceType", r.getPracticeType());
            item.put("totalQuestions", r.getTotalQuestions());
            item.put("correctCount", r.getCorrectCount());
            item.put("accuracyRate", r.getAccuracyRate());
            item.put("duration", r.getDuration());
            item.put("endTime", r.getEndTime());

            // 获取科目/知识点名称
            if (r.getSubjectId() != null) {
                Subject subject = subjectMapper.selectById(r.getSubjectId());
                if (subject != null) {
                    item.put("subjectName", subject.getName());
                }
            }
            if (r.getKnowledgeId() != null) {
                KnowledgePoint point = knowledgePointMapper.selectById(r.getKnowledgeId());
                if (point != null) {
                    item.put("knowledgeName", point.getName());
                }
            }

            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 获取练习统计
     */
    public Map<String, Object> getPracticeStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 按类型统计
        List<Map<String, Object>> typeStats = practiceRecordMapper.getPracticeStatsByType(userId);
        stats.put("typeStats", typeStats);

        // 总练习次数
        Long totalPractices = practiceRecordMapper.selectCount(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, 1)
        );
        stats.put("totalPractices", totalPractices);

        // 总答题数
        Long totalAnswers = practiceAnswerMapper.selectCount(
                new LambdaQueryWrapper<PracticeAnswer>().eq(PracticeAnswer::getUserId, userId)
        );
        stats.put("totalAnswers", totalAnswers);

        // 总正确数
        Long totalCorrect = practiceAnswerMapper.selectCount(
                new LambdaQueryWrapper<PracticeAnswer>()
                        .eq(PracticeAnswer::getUserId, userId)
                        .eq(PracticeAnswer::getIsCorrect, 1)
        );
        stats.put("totalCorrect", totalCorrect);

        List<PracticeRecord> completedRecords = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, 1)
        );
        int totalDuration = completedRecords.stream()
                .map(PracticeRecord::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        stats.put("totalDuration", totalDuration);

        // 总正确率
        if (totalAnswers > 0) {
            BigDecimal totalAccuracy = BigDecimal.valueOf(totalCorrect)
                    .divide(BigDecimal.valueOf(totalAnswers), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            stats.put("totalAccuracy", totalAccuracy);
        }

        // 最近练习
        List<Map<String, Object>> recentPractices = practiceRecordMapper.getRecentPractices(userId, 5);
        stats.put("recentPractices", recentPractices);

        return stats;
    }

    // ==================== 私有方法 ====================

    /**
     * 选择题目
     */
    private List<Question> selectQuestions(Long userId, PracticeRequest request) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        int limit = getPracticeQuestionLimit(request);

        if (request.getSubjectId() != null) {
            wrapper.eq(Question::getSubjectId, request.getSubjectId());
        }

        // 根据练习类型设置条件
        switch (request.getPracticeType()) {
            case "SUBJECT":
                break;
            case "KNOWLEDGE":
                if (request.getKnowledgeId() != null) {
                    wrapper.like(Question::getKnowledgeIds, request.getKnowledgeId().toString());
                }
                break;
            case "TYPE":
                if (request.getQuestionType() != null) {
                    wrapper.eq(Question::getType, request.getQuestionType());
                }
                break;
            case "MISTAKE":
                // 错题专项：从错题本获取题目
                if (request.getQuestionId() != null) {
                    Long count = mistakeBookMapper.selectCount(
                            new LambdaQueryWrapper<MistakeBook>()
                                    .eq(MistakeBook::getUserId, userId)
                                    .eq(MistakeBook::getQuestionId, request.getQuestionId())
                    );
                    if (count == 0) {
                        return Collections.emptyList();
                    }
                    wrapper.eq(Question::getId, request.getQuestionId());
                    break;
                }

                List<MistakeBook> mistakes = mistakeBookMapper.selectList(
                        new LambdaQueryWrapper<MistakeBook>()
                                .eq(MistakeBook::getUserId, userId)
                                .lt(MistakeBook::getMasteryLevel, 4)
                                .orderByAsc(MistakeBook::getMasteryLevel)
                                .last("LIMIT " + limit)
                );
                if (!mistakes.isEmpty()) {
                    List<Long> questionIds = mistakes.stream()
                            .map(MistakeBook::getQuestionId)
                            .collect(Collectors.toList());
                    wrapper.in(Question::getId, questionIds);
                } else {
                    return Collections.emptyList();
                }
                break;
        }

        // 排除已做对的题
        if (Boolean.TRUE.equals(request.getExcludeCorrect()) && !"MISTAKE".equals(request.getPracticeType())) {
            List<Long> correctQuestionIds = getCorrectQuestionIds(userId);
            if (!correctQuestionIds.isEmpty()) {
                wrapper.notIn(Question::getId, correctQuestionIds);
            }
        }

        // 随机排序并限制数量
        wrapper.last("ORDER BY RAND() LIMIT " + limit);

        List<Question> selected = questionMapper.selectList(wrapper);
        return padQuestionsIfNeeded(selected, limit);
    }

    private int getPracticeQuestionLimit(PracticeRequest request) {
        if ("TYPE".equals(request.getPracticeType())) {
            if (request.getLimit() != null) {
                return Math.min(Math.max(request.getLimit(), 1), 50);
            }
            Subject subject = request.getSubjectId() == null ? null : subjectMapper.selectById(request.getSubjectId());
            String subjectName = subject == null ? "" : subject.getName();
            return getQuestionTypeLimit(subjectName, request.getQuestionType());
        }
        return request.getLimit() != null ? Math.min(Math.max(request.getLimit(), 1), 50) : 10;
    }

    private int getQuestionTypeLimit(String subjectName, String questionType) {
        if ("SINGLE_CHOICE".equals(questionType)) {
            return 7;
        }
        if ("MULTI_CHOICE".equals(questionType)) {
            return 4;
        }
        return 3;
    }

    private List<Question> padQuestionsIfNeeded(List<Question> questions, int limit) {
        if (questions.isEmpty() || questions.size() >= limit) {
            return questions;
        }

        List<Question> padded = new ArrayList<>(questions);
        int index = 0;
        while (padded.size() < limit) {
            padded.add(questions.get(index % questions.size()));
            index++;
        }
        return padded;
    }

    /**
     * 获取用户已做对的题目ID列表
     */
    private List<Long> getCorrectQuestionIds(Long userId) {
        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getUserId, userId)
                        .eq(AnswerRecord::getIsCorrect, 1)
                        .select(AnswerRecord::getQuestionId)
        );
        return records.stream().map(AnswerRecord::getQuestionId).distinct().collect(Collectors.toList());
    }

    private String getQuestionTypeDisplayName(String questionType, Long subjectId) {
        String subjectName = "";
        if (subjectId != null) {
            Subject subject = subjectMapper.selectById(subjectId);
            subjectName = subject != null ? subject.getName() : "";
        }
        if ("数学".equals(subjectName) && "ESSAY".equals(questionType)) {
            return "计算题";
        }
        if (Set.of("生物", "化学", "物理").contains(subjectName)) {
            if ("SINGLE_CHOICE".equals(questionType)) {
                return "选择题";
            }
            if ("ESSAY".equals(questionType)) {
                return "非选择题";
            }
        }
        Map<String, String> typeNames = Map.of(
                "SINGLE_CHOICE", "单选题",
                "MULTI_CHOICE", "多选题",
                "TRUE_FALSE", "判断题",
                "FILL_BLANK", "填空题",
                "SHORT_ANSWER", "简答题",
                "ESSAY", "论述题"
        );
        return typeNames.getOrDefault(questionType, "题型");
    }

    /**
     * 生成练习名称
     */
    private String generatePracticeName(PracticeRequest request) {
        switch (request.getPracticeType()) {
            case "SUBJECT":
                Subject subject = subjectMapper.selectById(request.getSubjectId());
                return subject != null ? subject.getName() + "专项练习" : "科目专项练习";
            case "KNOWLEDGE":
                KnowledgePoint point = knowledgePointMapper.selectById(request.getKnowledgeId());
                return point != null ? point.getName() + "专项练习" : "知识点专项练习";
            case "TYPE":
                return getQuestionTypeDisplayName(request.getQuestionType(), request.getSubjectId()) + "专项练习";
            case "MISTAKE":
                return "错题专项练习";
            default:
                return "专项练习";
        }
    }

    /**
     * 获取知识点名称列表
     */
    private List<String> getKnowledgeNames(String knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        String[] ids = knowledgeIds.split(",");
        for (String id : ids) {
            try {
                KnowledgePoint point = knowledgePointMapper.selectById(Long.parseLong(id.trim()));
                if (point != null) {
                    names.add(point.getName());
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return names;
    }

    /**
     * 获取科目名称
     */
    private String getSubjectName(Long subjectId) {
        if (subjectId == null) return null;
        Subject subject = subjectMapper.selectById(subjectId);
        return subject != null ? subject.getName() : null;
    }

    private GradingResultResponse gradePracticeAnswer(Question question, String userAnswer) {
        if (isSubjectiveQuestion(question.getType())) {
            try {
                return claudeService.gradeSubjectiveQuestion(question, userAnswer == null ? "" : userAnswer);
            } catch (Exception ex) {
                return claudeService.strictSubjectiveFallback(question, userAnswer,
                        "AI批改暂不可用，系统已使用严格兜底判分：" + ex.getMessage());
            }
        }

        boolean isCorrect = checkAnswer(question, userAnswer);
        GradingResultResponse result = new GradingResultResponse();
        result.setQuestionId(question.getId());
        result.setIsCorrect(isCorrect);
        result.setScore(isCorrect ? BigDecimal.TEN : BigDecimal.ZERO);
        result.setMaxScore(BigDecimal.TEN);
        result.setCorrectAnswer(question.getAnswer());
        result.setAnalysis(question.getAnalysis());
        result.setMistakeType(isCorrect ? "CORRECT" : determineMistakeType(question, userAnswer));
        result.setAiAnalysis(isCorrect ? "回答正确。" : "答案错误，请结合解析订正。");
        return result;
    }

    private boolean isSubjectiveQuestion(String type) {
        return "SHORT_ANSWER".equals(type) || "ESSAY".equals(type);
    }

    /**
     * 检查答案是否正确
     */
    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        String correctAnswer = question.getAnswer();
        if (correctAnswer == null) {
            return false;
        }

        // 选择题、判断题：精确匹配
        if ("SINGLE_CHOICE".equals(question.getType()) ||
            "TRUE_FALSE".equals(question.getType()) ||
            "MULTI_CHOICE".equals(question.getType())) {
            return userAnswer.trim().toUpperCase().equals(correctAnswer.trim().toUpperCase());
        }

        // 填空题：忽略大小写和空格
        if ("FILL_BLANK".equals(question.getType())) {
            return userAnswer.trim().replaceAll("\\s+", "")
                    .equalsIgnoreCase(correctAnswer.trim().replaceAll("\\s+", ""));
        }

        // 主观题由 DeepSeek 批改；如果走到这里，严格判错，避免短答案被“包含匹配”误判。
        return false;
    }

    /**
     * 确定错误类型
     */
    private String determineMistakeType(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return "NO_ANSWER";
        }

        String correctAnswer = question.getAnswer();
        if (correctAnswer == null) {
            return "UNKNOWN";
        }

        // 简化的错误类型判断
        if ("SINGLE_CHOICE".equals(question.getType()) || "MULTI_CHOICE".equals(question.getType())) {
            return "CONCEPT_ERROR";
        } else if ("TRUE_FALSE".equals(question.getType())) {
            return "JUDGMENT_ERROR";
        } else if ("FILL_BLANK".equals(question.getType())) {
            return "MEMORY_ERROR";
        } else {
            return "UNDERSTANDING_ERROR";
        }
    }

    /**
     * 添加到错题本
     */
    private void addToMistakeBook(Long userId, Long questionId, String mistakeType) {
        // 检查是否已存在
        Long count = mistakeBookMapper.selectCount(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .eq(MistakeBook::getQuestionId, questionId)
        );

        if (count == 0) {
            MistakeBook mistake = new MistakeBook();
            mistake.setUserId(userId);
            mistake.setQuestionId(questionId);
            mistake.setMistakeType(mistakeType);
            mistake.setReviewCount(0);
            mistake.setMasteryLevel(0);
            mistake.setNextReviewDate(java.time.LocalDate.now().plusDays(1));
            mistake.setCreatedAt(LocalDateTime.now());
            mistakeBookMapper.insert(mistake);
        }
    }

    /**
     * 练习答题后更新知识点掌握度
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
                mastery.setMasteryLevel(BigDecimal.valueOf(correct)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
                mastery.setLastPracticeAt(LocalDateTime.now());
                knowledgeMasteryMapper.updateById(mastery);
            }
        }
    }

    /**
     * 获取题目顺序
     */
    private Integer getQuestionOrder(Map<String, Object> config, Long questionId) {
        @SuppressWarnings("unchecked")
        List<Long> questionIds = (List<Long>) config.get("questionIds");
        if (questionIds != null) {
            int index = questionIds.indexOf(questionId);
            return index >= 0 ? index + 1 : 1;
        }
        return 1;
    }

    /**
     * 获取错误知识点统计
     */
    private List<Map<String, Object>> getErrorKnowledgeStats(List<PracticeAnswer> answers) {
        List<PracticeAnswer> wrongAnswers = answers.stream()
                .filter(a -> a.getIsCorrect() == 0)
                .collect(Collectors.toList());

        Map<Long, Integer> knowledgeCount = new HashMap<>();
        for (PracticeAnswer answer : wrongAnswers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question != null && question.getKnowledgeIds() != null) {
                String[] ids = question.getKnowledgeIds().split(",");
                for (String id : ids) {
                    try {
                        Long kid = Long.parseLong(id.trim());
                        knowledgeCount.merge(kid, 1, Integer::sum);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : knowledgeCount.entrySet()) {
            KnowledgePoint point = knowledgePointMapper.selectById(entry.getKey());
            if (point != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("knowledgeId", entry.getKey());
                item.put("knowledgeName", point.getName());
                item.put("count", entry.getValue());
                result.add(item);
            }
        }

        result.sort((a, b) -> (Integer) b.get("count") - (Integer) a.get("count"));
        return result.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * 获取错误类型统计
     */
    private List<Map<String, Object>> getErrorTypeStats(List<PracticeAnswer> answers) {
        List<PracticeAnswer> wrongAnswers = answers.stream()
                .filter(a -> a.getIsCorrect() == 0)
                .collect(Collectors.toList());

        Map<String, Long> typeCount = wrongAnswers.stream()
                .filter(a -> a.getMistakeType() != null)
                .collect(Collectors.groupingBy(PracticeAnswer::getMistakeType, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, String> typeNames = Map.of(
                "CONCEPT_ERROR", "概念理解错误",
                "JUDGMENT_ERROR", "判断错误",
                "MEMORY_ERROR", "记忆错误",
                "UNDERSTANDING_ERROR", "理解偏差",
                "NO_ANSWER", "未作答",
                "UNKNOWN", "其他"
        );

        typeCount.forEach((type, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", type);
            item.put("name", typeNames.getOrDefault(type, type));
            item.put("count", count);
            result.add(item);
        });

        return result;
    }

    /**
     * 生成AI学习建议
     */
    private String generateAiSuggestion(PracticeRecord record, List<PracticeAnswer> answers) {
        StringBuilder sb = new StringBuilder();

        int correctCount = (int) answers.stream().filter(a -> a.getIsCorrect() == 1).count();
        int total = answers.size();
        double accuracy = total > 0 ? (double) correctCount / total * 100 : 0;

        if (accuracy >= 90) {
            sb.append("优秀！你对这部分内容掌握得很好。建议尝试更高难度的练习来进一步提升。\n");
        } else if (accuracy >= 70) {
            sb.append("良好！基本掌握了这部分内容。建议复习错题，巩固薄弱知识点。\n");
        } else if (accuracy >= 60) {
            sb.append("及格。对这部分内容有一定了解，但还需要加强练习。建议重点复习错题涉及的知识点。\n");
        } else {
            sb.append("需要加强。建议重新学习相关知识点，多做基础练习题。\n");
        }

        // 添加具体建议
        List<Map<String, Object>> errorKnowledge = getErrorKnowledgeStats(answers);
        if (!errorKnowledge.isEmpty()) {
            sb.append("\n薄弱知识点：");
            for (Map<String, Object> item : errorKnowledge) {
                sb.append(item.get("knowledgeName")).append("、");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("。建议重点复习这些内容。");
        }
        return sb.toString();
    }

    /**
     * 按学科分类构建答题详情
     */
    private List<PracticeResultResponse.SubjectResult> buildSubjectResults(List<PracticeAnswer> answers) {
        Map<Long, List<PracticeAnswer>> subjectAnswersMap = new LinkedHashMap<>();

        // 按学科分组
        for (PracticeAnswer answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question != null) {
                Long subjectId = question.getSubjectId();
                subjectAnswersMap.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(answer);
            }
        }

        List<PracticeResultResponse.SubjectResult> subjectResults = new ArrayList<>();

        for (Map.Entry<Long, List<PracticeAnswer>> entry : subjectAnswersMap.entrySet()) {
            Long subjectId = entry.getKey();
            List<PracticeAnswer> subjectAnswers = entry.getValue();

            Subject subject = subjectMapper.selectById(subjectId);
            if (subject == null) continue;

            PracticeResultResponse.SubjectResult sr = new PracticeResultResponse.SubjectResult();
            sr.setSubjectId(subjectId);
            sr.setSubjectName(subject.getName());
            sr.setSubjectIcon(subject.getIcon());
            sr.setTotalQuestions(subjectAnswers.size());

            int subjectCorrect = (int) subjectAnswers.stream().filter(a -> a.getIsCorrect() == 1).count();
            int subjectWrong = subjectAnswers.size() - subjectCorrect;

            sr.setCorrectCount(subjectCorrect);
            sr.setWrongCount(subjectWrong);

            BigDecimal subjectAccuracy = BigDecimal.ZERO;
            if (!subjectAnswers.isEmpty()) {
                subjectAccuracy = BigDecimal.valueOf(subjectCorrect)
                        .divide(BigDecimal.valueOf(subjectAnswers.size()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            sr.setAccuracyRate(subjectAccuracy);

            BigDecimal subjectScore = subjectAnswers.stream()
                    .map(a -> a.getScore() != null ? a.getScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal subjectMaxScore = BigDecimal.valueOf(subjectAnswers.size() * 10);

            sr.setTotalScore(subjectScore);
            sr.setMaxScore(subjectMaxScore);

            // 构建该学科的题目列表
            List<PracticeResultResponse.QuestionResult> questions = new ArrayList<>();
            for (PracticeAnswer answer : subjectAnswers) {
                Question question = questionMapper.selectById(answer.getQuestionId());
                if (question != null) {
                    PracticeResultResponse.QuestionResult qr = new PracticeResultResponse.QuestionResult();
                    qr.setQuestionId(answer.getQuestionId());
                    qr.setOrder(answer.getQuestionOrder());
                    qr.setQuestionContent(question.getContent());
                    qr.setUserAnswer(answer.getUserAnswer());
                    qr.setCorrectAnswer(question.getAnswer());
                    qr.setIsCorrect(answer.getIsCorrect() == 1);
                    qr.setScore(answer.getScore());
                    qr.setMaxScore(BigDecimal.TEN);
                    qr.setMistakeType(answer.getMistakeType());
                    qr.setAiAnalysis(answer.getAiAnalysis());
                    questions.add(qr);
                }
            }
            sr.setQuestions(questions);

            subjectResults.add(sr);
        }

        return subjectResults;
    }
}
