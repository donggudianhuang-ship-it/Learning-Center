package org.example.smartlearning.service.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.entity.*;
import org.example.smartlearning.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习路径服务类
 */
@Service
@RequiredArgsConstructor
public class LearningPathService {

    private final MistakeBookMapper mistakeBookMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final UserMapper userMapper;
    private final KnowledgeTaggingService knowledgeTaggingService;
    private final PracticeRecordMapper practiceRecordMapper;

    /**
     * 获取错题本列表
     */
    public List<Map<String, Object>> getMistakeBook(Long userId, String subjectId, Integer page, Integer size) {
        LambdaQueryWrapper<MistakeBook> wrapper = new LambdaQueryWrapper<MistakeBook>()
                .eq(MistakeBook::getUserId, userId)
                .orderByDesc(MistakeBook::getCreatedAt);

        List<MistakeBook> mistakes = mistakeBookMapper.selectList(wrapper);
        if (subjectId != null && !subjectId.isBlank()) {
            try {
                Long filterSubjectId = Long.parseLong(subjectId);
                mistakes = mistakes.stream()
                        .filter(mistake -> {
                            Question question = questionMapper.selectById(mistake.getQuestionId());
                            return question != null && filterSubjectId.equals(question.getSubjectId());
                        })
                        .collect(Collectors.toList());
            } catch (NumberFormatException ignored) {
            }
        }

        int safePage = page != null && page > 0 ? page : 1;
        int safeSize = size != null && size > 0 ? Math.min(size, 100) : 20;
        int fromIndex = Math.min((safePage - 1) * safeSize, mistakes.size());
        int toIndex = Math.min(fromIndex + safeSize, mistakes.size());
        mistakes = mistakes.subList(fromIndex, toIndex);

        return mistakes.stream().map(mistake -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", mistake.getId());
            map.put("questionId", mistake.getQuestionId());
            map.put("mistakeType", mistake.getMistakeType());
            map.put("reviewCount", mistake.getReviewCount());
            map.put("masteryLevel", mistake.getMasteryLevel());
            map.put("nextReviewDate", mistake.getNextReviewDate());

            Question question = questionMapper.selectById(mistake.getQuestionId());
            if (question != null) {
                map.put("questionContent", question.getContent());
                map.put("questionType", question.getType());
                map.put("difficulty", question.getDifficulty());
                map.put("subjectId", question.getSubjectId());
            }

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 智能推荐题目
     */
    public List<Map<String, Object>> recommendQuestions(Long userId, Integer limit) {
        int safeLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 10;
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 1. 错题复习优先。到期错题比新题更需要及时处理。
        List<MistakeBook> dueReviews = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .le(MistakeBook::getNextReviewDate, LocalDate.now())
                        .orderByAsc(MistakeBook::getNextReviewDate)
                        .last("LIMIT 5")
        );

        for (MistakeBook mistake : dueReviews) {
            Question question = questionMapper.selectById(mistake.getQuestionId());
            if (question != null) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("question", question);
                rec.put("reason", "错题到期复盘");
                rec.put("mistakeType", mistake.getMistakeType());
                rec.put("knowledgeNames", knowledgeTaggingService.resolveKnowledgeNames(question));
                rec.put("priority", 1);
                recommendations.add(rec);
            }
        }

        // 2. 基于薄弱知识点推荐，使用 FIND_IN_SET 精确匹配 knowledge_ids。
        List<KnowledgeMastery> weakPoints = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .lt(KnowledgeMastery::getMasteryLevel, 75)
                        .orderByAsc(KnowledgeMastery::getMasteryLevel)
                        .last("LIMIT 3")
        );

        for (KnowledgeMastery mastery : weakPoints) {
            List<Question> questions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .apply("FIND_IN_SET({0}, knowledge_ids)", mastery.getKnowledgeId())
                            .orderByAsc(Question::getDifficulty)
                            .last("LIMIT 5")
            );

            for (Question q : questions) {
                Long correctCount = answerRecordMapper.selectCount(
                        new LambdaQueryWrapper<AnswerRecord>()
                                .eq(AnswerRecord::getUserId, userId)
                                .eq(AnswerRecord::getQuestionId, q.getId())
                                .eq(AnswerRecord::getIsCorrect, 1)
                );

                if (correctCount == 0) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("question", q);
                    rec.put("reason", "薄弱知识点练习");
                    KnowledgePoint point = knowledgePointMapper.selectById(mastery.getKnowledgeId());
                    rec.put("knowledgeName", point != null ? point.getName() : "");
                    rec.put("knowledgeNames", knowledgeTaggingService.resolveKnowledgeNames(q));
                    rec.put("masteryLevel", mastery.getMasteryLevel());
                    rec.put("priority", 2);
                    recommendations.add(rec);
                }
            }
        }

        // 3. 如果是新用户（没有推荐结果），随机推荐题目
        if (recommendations.isEmpty()) {
            // 获取用户已做过的题目ID
            List<Long> doneQuestionIds = answerRecordMapper.selectList(
                    new LambdaQueryWrapper<AnswerRecord>()
                            .eq(AnswerRecord::getUserId, userId)
                            .select(AnswerRecord::getQuestionId)
            ).stream().map(AnswerRecord::getQuestionId).distinct().collect(Collectors.toList());

            // 查询未做过的题目
            LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<Question>();
            if (!doneQuestionIds.isEmpty()) {
                questionWrapper.notIn(Question::getId, doneQuestionIds);
            }
            questionWrapper.last("ORDER BY RAND() LIMIT " + safeLimit);

            List<Question> randomQuestions = questionMapper.selectList(questionWrapper);

            for (Question q : randomQuestions) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("question", q);
                rec.put("reason", "诊断练习");
                rec.put("knowledgeNames", knowledgeTaggingService.resolveKnowledgeNames(q));
                rec.put("priority", 3);
                recommendations.add(rec);
            }
        }

        Set<Long> seenQuestionIds = new HashSet<>();
        return recommendations.stream()
                .sorted(Comparator.comparing(r -> (Integer) r.get("priority")))
                .filter(rec -> {
                    Question question = (Question) rec.get("question");
                    return question != null && seenQuestionIds.add(question.getId());
                })
                .limit(safeLimit)
                .collect(Collectors.toList());
    }

    /**
     * 获取学生画像、阶段目标与个性化学习路径
     */
    public Map<String, Object> getPersonalizedPath(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        User user = userMapper.selectById(userId);
        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getUserId, userId)
                        .orderByDesc(AnswerRecord::getCreatedAt)
        );
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
        );
        List<MistakeBook> allMistakes = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
        );
        List<PracticeRecord> completedPractices = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, 1)
        );

        long total = records.size();
        long correct = records.stream().filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 1).count();
        BigDecimal accuracyRate = percent(correct, total);
        BigDecimal recentAccuracyRate = calculateRecentAccuracy(records);
        long unlabeledAnswerCount = 0;
        Set<Long> taggedKnowledgeIds = loadTaggedKnowledgeIds(masteries);
        int practicedKnowledgeCount = (int) masteries.stream()
                .map(KnowledgeMastery::getKnowledgeId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        BigDecimal coverageRate = percent(practicedKnowledgeCount, taggedKnowledgeIds.size());
        int masteredKnowledgeCount = (int) masteries.stream()
                .filter(mastery -> masteryValue(mastery) >= 80)
                .count();
        int weakKnowledgeCount = (int) masteries.stream()
                .filter(mastery -> masteryValue(mastery) < 60)
                .count();
        int totalStudyMinutes = calculateStudyMinutes(total, completedPractices);

        List<KnowledgeMastery> weakMasteries = masteries.stream()
                .filter(mastery -> masteryValue(mastery) < 80)
                .sorted(Comparator.comparingDouble((KnowledgeMastery mastery) -> 100 - masteryValue(mastery)).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<MistakeBook> dueReviews = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .le(MistakeBook::getNextReviewDate, LocalDate.now())
                        .orderByAsc(MistakeBook::getNextReviewDate)
                        .last("LIMIT 5")
        );

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("nickname", user != null ? user.getNickname() : "同学");
        profile.put("grade", user != null ? user.getGrade() : null);
        profile.put("answeredQuestions", total);
        profile.put("accuracyRate", accuracyRate);
        profile.put("recentAccuracyRate", recentAccuracyRate);
        profile.put("unlabeledAnswerCount", unlabeledAnswerCount);
        profile.put("totalStudyMinutes", totalStudyMinutes);
        profile.put("activeDays", calculateActiveDays(records, completedPractices));
        profile.put("coverageRate", coverageRate);
        profile.put("practicedKnowledgeCount", practicedKnowledgeCount);
        profile.put("totalTaggedKnowledgeCount", taggedKnowledgeIds.size());
        profile.put("masteredKnowledgeCount", masteredKnowledgeCount);
        profile.put("weakKnowledgeCount", weakKnowledgeCount);
        profile.put("dueReviewCount", dueReviews.size());
        profile.put("mistakeCount", allMistakes.size());
        profile.put("cognitiveStyle", inferCognitiveStyle(records, weakMasteries, allMistakes));
        profile.put("learningStage", inferLearningStage(total, accuracyRate, weakKnowledgeCount, coverageRate));
        profile.put("weakestPoint", weakMasteries.isEmpty() ? "暂无明显薄弱点" : getKnowledgeName(weakMasteries.get(0).getKnowledgeId()));
        profile.put("selfRegulation", dueReviews.isEmpty() ? "复盘节奏稳定" : "需要完成今日复盘");
        profile.put("dataConfidence", inferDataConfidence(total, practicedKnowledgeCount, coverageRate));
        result.put("profile", profile);

        Map<String, Object> stageGoal = new LinkedHashMap<>();
        KnowledgeMastery topWeak = weakMasteries.isEmpty() ? null : weakMasteries.get(0);
        stageGoal.put("title", topWeak == null ? "保持稳定输出" : "修复核心薄弱点");
        stageGoal.put("target", topWeak == null
                ? "本周完成一次综合练习，并把正确率稳定在80%以上"
                : "优先把 " + getKnowledgeName(topWeak.getKnowledgeId()) + " 掌握度提升到75%以上");
        stageGoal.put("progress", topWeak == null ? accuracyRate : normalize(topWeak.getMasteryLevel()));
        stageGoal.put("targetProgress", topWeak == null ? BigDecimal.valueOf(80) : BigDecimal.valueOf(75));
        stageGoal.put("targetKnowledgeId", topWeak == null ? null : topWeak.getKnowledgeId());
        stageGoal.put("basis", buildStageGoalBasis(total, accuracyRate, recentAccuracyRate, topWeak, dueReviews.size()));
        stageGoal.put("deadline", LocalDate.now().plusDays(7));
        result.put("stageGoal", stageGoal);

        result.put("tasks", buildPathTasks(weakMasteries, dueReviews, total, accuracyRate, recentAccuracyRate, coverageRate));
        result.put("pathSteps", buildPathSteps(weakMasteries, dueReviews, total, accuracyRate, coverageRate));
        result.put("reflectionPrompt", buildReflectionPrompt(accuracyRate, dueReviews.size(), weakMasteries));
        result.put("recentMistakes", getRecentMistakes(userId));

        return result;
    }

    /**
     * 生成复习计划
     */
    public Map<String, Object> generateReviewPlan(Long userId) {
        Map<String, Object> plan = new HashMap<>();

        // 获取所有需要复习的错题
        List<MistakeBook> mistakes = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .orderByAsc(MistakeBook::getNextReviewDate)
        );

        // 按日期分组
        Map<LocalDate, List<MistakeBook>> groupedByDate = mistakes.stream()
                .filter(m -> m.getNextReviewDate() != null)
                .collect(Collectors.groupingBy(MistakeBook::getNextReviewDate));

        List<Map<String, Object>> dailyPlans = new ArrayList<>();
        for (Map.Entry<LocalDate, List<MistakeBook>> entry : groupedByDate.entrySet()) {
            Map<String, Object> dailyPlan = new HashMap<>();
            dailyPlan.put("date", entry.getKey());
            dailyPlan.put("count", entry.getValue().size());

            List<Map<String, Object>> questions = entry.getValue().stream().map(m -> {
                Map<String, Object> q = new HashMap<>();
                q.put("mistakeId", m.getId());
                q.put("questionId", m.getQuestionId());
                q.put("masteryLevel", m.getMasteryLevel());

                Question question = questionMapper.selectById(m.getQuestionId());
                if (question != null) {
                    q.put("content", question.getContent());
                    q.put("type", question.getType());
                }

                return q;
            }).collect(Collectors.toList());

            dailyPlan.put("questions", questions);
            dailyPlans.add(dailyPlan);
        }

        plan.put("dailyPlans", dailyPlans);
        plan.put("totalMistakes", mistakes.size());

        return plan;
    }

    /**
     * 更新错题复习状态
     */
    @Transactional
    public void reviewMistake(Long userId, Long mistakeId, boolean isCorrect) {
        MistakeBook mistake = mistakeBookMapper.selectById(mistakeId);
        if (mistake == null || !mistake.getUserId().equals(userId)) {
            throw new RuntimeException("错题记录不存在");
        }

        mistake.setReviewCount(mistake.getReviewCount() + 1);
        mistake.setLastReviewAt(LocalDateTime.now());

        if (isCorrect) {
            // 答对了，提高掌握度
            int newLevel = Math.min(5, mistake.getMasteryLevel() + 1);
            mistake.setMasteryLevel(newLevel);

            // 根据掌握度设置下次复习时间
            int daysToAdd = (int) Math.pow(2, newLevel); // 艾宾浩斯遗忘曲线
            mistake.setNextReviewDate(LocalDate.now().plusDays(daysToAdd));
        } else {
            // 答错了，重置掌握度
            mistake.setMasteryLevel(0);
            mistake.setNextReviewDate(LocalDate.now().plusDays(1));
        }

        mistakeBookMapper.updateById(mistake);
    }

    /**
     * 获取学习进度统计
     */
    public Map<String, Object> getLearningProgress(Long userId) {
        Map<String, Object> progress = new HashMap<>();

        // 总错题数
        Long totalMistakes = mistakeBookMapper.selectCount(
                new LambdaQueryWrapper<MistakeBook>().eq(MistakeBook::getUserId, userId)
        );
        progress.put("totalMistakes", totalMistakes);

        // 已掌握错题数
        Long masteredMistakes = mistakeBookMapper.selectCount(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .ge(MistakeBook::getMasteryLevel, 4)
        );
        progress.put("masteredMistakes", masteredMistakes);

        // 今日待复习
        Long todayReview = mistakeBookMapper.selectCount(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .le(MistakeBook::getNextReviewDate, LocalDate.now())
        );
        progress.put("todayReview", todayReview);

        // 知识点掌握统计
        Long totalKnowledge = knowledgeMasteryMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeMastery>().eq(KnowledgeMastery::getUserId, userId)
        );
        Long masteredKnowledge = knowledgeMasteryMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .ge(KnowledgeMastery::getMasteryLevel, 80)
        );

        progress.put("totalKnowledge", totalKnowledge);
        progress.put("masteredKnowledge", masteredKnowledge);

        return progress;
    }

    private List<Map<String, Object>> buildPathTasks(List<KnowledgeMastery> weakMasteries,
                                                     List<MistakeBook> dueReviews,
                                                     long totalAnswers,
                                                     BigDecimal accuracyRate,
                                                     BigDecimal recentAccuracyRate,
                                                     BigDecimal coverageRate) {
        List<Map<String, Object>> tasks = new ArrayList<>();

        if (totalAnswers == 0) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("type", "DIAGNOSIS");
            task.put("title", "完成首次诊断");
            task.put("description", "先做一组覆盖不同知识点的题，系统会据此建立掌握度画像。");
            task.put("priority", 1);
            task.put("estimatedMinutes", 20);
            task.put("progress", 0);
            tasks.add(task);
        }

        if (!dueReviews.isEmpty()) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("type", "REVIEW");
            task.put("title", "今日错题复盘");
            task.put("description", "完成 " + dueReviews.size() + " 道到期错题，先复述错因，再重做并检查关键步骤。");
            task.put("questionIds", dueReviews.stream().map(MistakeBook::getQuestionId).collect(Collectors.toList()));
            task.put("priority", tasks.size() + 1);
            task.put("estimatedMinutes", Math.min(40, Math.max(10, dueReviews.size() * 5)));
            task.put("progress", 0);
            tasks.add(task);
        }

        for (KnowledgeMastery mastery : weakMasteries.stream().limit(3).collect(Collectors.toList())) {
            Map<String, Object> task = new LinkedHashMap<>();
            String knowledgeName = getKnowledgeName(mastery.getKnowledgeId());
            task.put("type", "PRACTICE");
            task.put("title", knowledgeName + "专项提升");
            task.put("description", "当前掌握度 " + normalize(mastery.getMasteryLevel()) + "%，先做基础题确认概念，再做中档题巩固方法。");
            task.put("knowledgeId", mastery.getKnowledgeId());
            task.put("knowledgeName", knowledgeName);
            task.put("questionIds", findQuestionIdsByKnowledge(mastery.getKnowledgeId(), 5));
            task.put("priority", tasks.size() + 1);
            task.put("estimatedMinutes", 25);
            task.put("targetProgress", 75);
            task.put("progress", normalize(mastery.getMasteryLevel()));
            tasks.add(task);
        }

        if (recentAccuracyRate.compareTo(accuracyRate.subtract(BigDecimal.TEN)) < 0 && totalAnswers >= 5) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("type", "RECENT_ERROR_REVIEW");
            task.put("title", "复盘近期下滑题");
            task.put("description", "近7天正确率低于整体水平，建议回看最近错题，把错因分成概念、方法、审题三类。");
            task.put("priority", tasks.size() + 1);
            task.put("estimatedMinutes", 15);
            task.put("progress", recentAccuracyRate);
            tasks.add(task);
        }

        if (coverageRate.compareTo(BigDecimal.valueOf(70)) < 0 && totalAnswers > 0) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("type", "COVERAGE");
            task.put("title", "补齐知识点覆盖");
            task.put("description", "当前知识点覆盖率 " + coverageRate + "%，优先做未形成掌握度记录的知识点诊断题。");
            task.put("priority", tasks.size() + 1);
            task.put("estimatedMinutes", 20);
            task.put("progress", coverageRate);
            tasks.add(task);
        }

        Map<String, Object> reflection = new LinkedHashMap<>();
        reflection.put("type", "REFLECTION");
        reflection.put("title", "学习反思记录");
        reflection.put("description", totalAnswers == 0
                ? "先完成一次诊断练习，再写下最卡住你的知识点"
                : "用一句话总结今天最容易错的原因，并写入笔记");
        reflection.put("priority", tasks.size() + 1);
        reflection.put("estimatedMinutes", 5);
        reflection.put("progress", 0);
        tasks.add(reflection);

        if (tasks.size() < 3) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("type", "CHALLENGE");
            task.put("title", "综合提升训练");
            task.put("description", "目前没有明显高风险薄弱点，完成一组中高难度综合题保持手感。");
            task.put("priority", tasks.size() + 1);
            task.put("estimatedMinutes", 30);
            task.put("progress", 0);
            tasks.add(task);
        }

        return tasks.stream()
                .sorted(Comparator.comparing(task -> (Integer) task.get("priority")))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentMistakes(Long userId) {
        List<MistakeBook> mistakes = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .orderByDesc(MistakeBook::getCreatedAt)
                        .last("LIMIT 3")
        );

        return mistakes.stream().map(mistake -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", mistake.getId());
            item.put("questionId", mistake.getQuestionId());
            item.put("mistakeType", mistake.getMistakeType());
            item.put("masteryLevel", mistake.getMasteryLevel());
            Question question = questionMapper.selectById(mistake.getQuestionId());
            if (question != null) {
                item.put("questionContent", question.getContent());
                item.put("knowledgeNames", knowledgeTaggingService.resolveKnowledgeNames(question));
            }
            return item;
        }).collect(Collectors.toList());
    }

    private String inferCognitiveStyle(List<AnswerRecord> records,
                                       List<KnowledgeMastery> weakMasteries,
                                       List<MistakeBook> mistakes) {
        if (records.size() < 5) {
            return "待诊断型";
        }
        long subjectiveErrors = records.stream()
                .filter(r -> r.getMistakeType() != null)
                .filter(r -> r.getMistakeType().contains("INCOMPLETE") || r.getMistakeType().contains("WRONG_APPROACH"))
                .count();
        if (subjectiveErrors >= 2) {
            return "思路建构型";
        }
        long reviewBacklog = mistakes.stream()
                .filter(mistake -> mistake.getNextReviewDate() != null && !mistake.getNextReviewDate().isAfter(LocalDate.now()))
                .count();
        if (reviewBacklog >= 3) {
            return "复盘滞后型";
        }
        if (weakMasteries.size() >= 3) {
            return "基础巩固型";
        }
        return "练习驱动型";
    }

    private String inferLearningStage(long totalAnswers,
                                      BigDecimal accuracyRate,
                                      int weakCount,
                                      BigDecimal coverageRate) {
        if (totalAnswers < 5) {
            return "起步诊断期";
        }
        if (coverageRate.compareTo(BigDecimal.valueOf(60)) < 0) {
            return "诊断覆盖期";
        }
        if (accuracyRate.compareTo(BigDecimal.valueOf(85)) >= 0 && weakCount == 0) {
            return "拓展提升期";
        }
        if (accuracyRate.compareTo(BigDecimal.valueOf(65)) >= 0) {
            return "巩固修复期";
        }
        return "基础补强期";
    }

    private String buildReflectionPrompt(BigDecimal accuracyRate, int reviewCount, List<KnowledgeMastery> weakMasteries) {
        if (reviewCount > 0) {
            return "今天先复盘到期错题：你上次为什么会错？这次用什么检查方法避免重复失误？";
        }
        if (!weakMasteries.isEmpty()) {
            return "围绕“" + getKnowledgeName(weakMasteries.get(0).getKnowledgeId()) + "”写一句反思：概念不清、审题粗心还是解题路线不稳？";
        }
        if (accuracyRate.compareTo(BigDecimal.valueOf(85)) >= 0) {
            return "今天可以挑战一道高难度题，记录你从题干提取关键信息的过程。";
        }
        return "先完成一次诊断练习，系统会根据结果生成更具体的路径。";
    }

    private List<Map<String, Object>> buildPathSteps(List<KnowledgeMastery> weakMasteries,
                                                     List<MistakeBook> dueReviews,
                                                     long totalAnswers,
                                                     BigDecimal accuracyRate,
                                                     BigDecimal coverageRate) {
        List<Map<String, Object>> steps = new ArrayList<>();

        Map<String, Object> today = new LinkedHashMap<>();
        today.put("phase", "今日");
        today.put("title", dueReviews.isEmpty() ? "完成诊断/专项练习" : "先处理到期错题");
        today.put("goal", dueReviews.isEmpty()
                ? (totalAnswers < 5 ? "完成一组诊断题，建立初始画像" : "完成一个薄弱知识点专项练习")
                : "复盘 " + dueReviews.size() + " 道到期错题，记录错因");
        today.put("expectedOutcome", dueReviews.isEmpty() ? "获得更稳定的掌握度数据" : "降低重复失误");
        steps.add(today);

        Map<String, Object> thisWeek = new LinkedHashMap<>();
        KnowledgeMastery topWeak = weakMasteries.isEmpty() ? null : weakMasteries.get(0);
        thisWeek.put("phase", "本周");
        thisWeek.put("title", topWeak == null ? "综合训练保持手感" : "集中修复 " + getKnowledgeName(topWeak.getKnowledgeId()));
        thisWeek.put("goal", topWeak == null
                ? "完成一套综合练习，正确率保持在80%以上"
                : "把掌握度从 " + normalize(topWeak.getMasteryLevel()) + "% 提升到75%以上");
        thisWeek.put("expectedOutcome", "形成可复用的解题步骤和检查清单");
        steps.add(thisWeek);

        Map<String, Object> later = new LinkedHashMap<>();
        later.put("phase", "巩固");
        later.put("title", coverageRate.compareTo(BigDecimal.valueOf(70)) < 0 ? "补齐知识点覆盖" : "变式和限时训练");
        later.put("goal", coverageRate.compareTo(BigDecimal.valueOf(70)) < 0
                ? "覆盖更多已标注知识点，让路径推荐更准"
                : "用限时练习把正确率稳定到更高区间");
        later.put("expectedOutcome", accuracyRate.compareTo(BigDecimal.valueOf(80)) >= 0 ? "冲刺提升" : "稳定达到80%正确率");
        steps.add(later);

        return steps;
    }

    private String buildStageGoalBasis(long totalAnswers,
                                       BigDecimal accuracyRate,
                                       BigDecimal recentAccuracyRate,
                                       KnowledgeMastery topWeak,
                                       int dueReviewCount) {
        List<String> basis = new ArrayList<>();
        basis.add("累计答题 " + totalAnswers + " 道");
        basis.add("整体正确率 " + accuracyRate + "%");
        basis.add("近7天正确率 " + recentAccuracyRate + "%");
        if (topWeak != null) {
            basis.add(getKnowledgeName(topWeak.getKnowledgeId()) + " 掌握度 " + normalize(topWeak.getMasteryLevel()) + "%");
        }
        if (dueReviewCount > 0) {
            basis.add("今日到期错题 " + dueReviewCount + " 道");
        }
        return String.join("，", basis);
    }

    private BigDecimal calculateRecentAccuracy(List<AnswerRecord> records) {
        LocalDateTime start = LocalDate.now().minusDays(6).atStartOfDay();
        List<AnswerRecord> recentRecords = records.stream()
                .filter(record -> record.getCreatedAt() != null && !record.getCreatedAt().isBefore(start))
                .collect(Collectors.toList());
        long correct = recentRecords.stream()
                .filter(record -> record.getIsCorrect() != null && record.getIsCorrect() == 1)
                .count();
        return percent(correct, recentRecords.size());
    }

    private int calculateActiveDays(List<AnswerRecord> records, List<PracticeRecord> practices) {
        Set<LocalDate> dates = new HashSet<>();
        records.stream()
                .map(AnswerRecord::getCreatedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .forEach(dates::add);
        practices.stream()
                .map(PracticeRecord::getEndTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .forEach(dates::add);
        return dates.size();
    }

    private int calculateStudyMinutes(long totalAnswers, List<PracticeRecord> practices) {
        int durationSeconds = practices.stream()
                .map(PracticeRecord::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (durationSeconds > 0) {
            return Math.max(1, (int) Math.ceil(durationSeconds / 60.0));
        }
        return (int) totalAnswers * 5;
    }

    private Set<Long> loadTaggedKnowledgeIds(List<KnowledgeMastery> masteries) {
        Set<Long> ids = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .isNotNull(Question::getKnowledgeIds)
                        .ne(Question::getKnowledgeIds, "")
        ).stream()
                .flatMap(question -> parseKnowledgeIds(question.getKnowledgeIds()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ids.isEmpty()) {
            ids.addAll(masteries.stream()
                    .map(KnowledgeMastery::getKnowledgeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        return ids;
    }

    private List<Long> findQuestionIdsByKnowledge(Long knowledgeId, int limit) {
        if (knowledgeId == null) {
            return Collections.emptyList();
        }
        return questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .apply("FIND_IN_SET({0}, knowledge_ids)", knowledgeId)
                        .orderByAsc(Question::getDifficulty)
                        .last("LIMIT " + limit)
        ).stream().map(Question::getId).collect(Collectors.toList());
    }

    private double pathPriority(KnowledgeMastery mastery, List<MistakeBook> mistakes, List<AnswerRecord> records) {
        Long knowledgeId = mastery.getKnowledgeId();
        return (100 - masteryValue(mastery))
                + countMistakesForKnowledge(knowledgeId, mistakes, false) * 8.0
                + countMistakesForKnowledge(knowledgeId, mistakes, true) * 12.0
                + countRecentWrongForKnowledge(knowledgeId, records) * 10.0;
    }

    private long countMistakesForKnowledge(Long knowledgeId, List<MistakeBook> mistakes, boolean onlyDue) {
        if (knowledgeId == null) {
            return 0;
        }
        return mistakes.stream()
                .filter(mistake -> !onlyDue || (mistake.getNextReviewDate() != null && !mistake.getNextReviewDate().isAfter(LocalDate.now())))
                .filter(mistake -> {
                    Question question = questionMapper.selectById(mistake.getQuestionId());
                    return knowledgeTaggingService.resolveKnowledgeIds(question).contains(knowledgeId);
                })
                .count();
    }

    private long countRecentWrongForKnowledge(Long knowledgeId, List<AnswerRecord> records) {
        if (knowledgeId == null) {
            return 0;
        }
        LocalDateTime start = LocalDate.now().minusDays(6).atStartOfDay();
        return records.stream()
                .filter(record -> record.getCreatedAt() != null && !record.getCreatedAt().isBefore(start))
                .filter(record -> record.getIsCorrect() == null || record.getIsCorrect() == 0)
                .filter(record -> {
                    Question question = questionMapper.selectById(record.getQuestionId());
                    return knowledgeTaggingService.resolveKnowledgeIds(question).contains(knowledgeId);
                })
                .count();
    }

    private String inferDataConfidence(long totalAnswers, int practicedKnowledgeCount, BigDecimal coverageRate) {
        if (totalAnswers < 5 || practicedKnowledgeCount < 2) {
            return "推荐依据：样本较少，先完成一轮诊断练习后推荐会更准";
        }
        if (totalAnswers < 20 || coverageRate.compareTo(BigDecimal.valueOf(60)) < 0) {
            return "推荐依据：已有参考记录，继续补齐知识点覆盖后推荐会更准";
        }
        return "推荐依据：数据较充分，已基于 " + totalAnswers + " 道答题记录和 "
                + coverageRate + "% 知识点覆盖生成";
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(1, RoundingMode.HALF_UP);
    }

    private double masteryValue(KnowledgeMastery mastery) {
        return Optional.ofNullable(mastery.getMasteryLevel()).orElse(BigDecimal.ZERO).doubleValue();
    }

    private Set<Long> parseKnowledgeIds(String knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isBlank()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String id : knowledgeIds.split(",")) {
            try {
                ids.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private String getKnowledgeName(Long knowledgeId) {
        KnowledgePoint point = knowledgePointMapper.selectById(knowledgeId);
        return point != null ? point.getName() : "知识点" + knowledgeId;
    }

    private List<String> getKnowledgeNames(String knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isBlank()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (String id : knowledgeIds.split(",")) {
            try {
                names.add(getKnowledgeName(Long.parseLong(id.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        return names;
    }
}
