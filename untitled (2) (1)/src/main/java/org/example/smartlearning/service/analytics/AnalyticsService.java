package org.example.smartlearning.service.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.LearningAnalyticsResponse;
import org.example.smartlearning.entity.AnswerRecord;
import org.example.smartlearning.entity.KnowledgeMastery;
import org.example.smartlearning.entity.KnowledgePoint;
import org.example.smartlearning.entity.MistakeBook;
import org.example.smartlearning.entity.PracticeRecord;
import org.example.smartlearning.entity.Question;
import org.example.smartlearning.entity.Subject;
import org.example.smartlearning.mapper.AnswerRecordMapper;
import org.example.smartlearning.mapper.KnowledgeMasteryMapper;
import org.example.smartlearning.mapper.KnowledgePointMapper;
import org.example.smartlearning.mapper.MistakeBookMapper;
import org.example.smartlearning.mapper.PracticeRecordMapper;
import org.example.smartlearning.mapper.QuestionMapper;
import org.example.smartlearning.mapper.SubjectMapper;
import org.example.smartlearning.service.learning.KnowledgeTaggingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学情分析服务类
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RECENT_DAYS = 7;

    private final AnswerRecordMapper answerRecordMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final MistakeBookMapper mistakeBookMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final KnowledgeTaggingService knowledgeTaggingService;

    /**
     * 获取学情分析报告
     */
    public LearningAnalyticsResponse getLearningAnalytics(Long userId) {
        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getUserId, userId)
                        .orderByDesc(AnswerRecord::getCreatedAt)
        );
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .orderByAsc(KnowledgeMastery::getMasteryLevel)
        );
        List<MistakeBook> mistakes = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
                        .orderByDesc(MistakeBook::getCreatedAt)
        );
        List<PracticeRecord> completedPractices = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, 1)
        );

        Map<Long, KnowledgePoint> knowledgeById = loadKnowledgeById(masteries);
        Map<Long, Subject> subjectById = loadSubjectsByKnowledge(knowledgeById);
        Set<Long> taggedKnowledgeIds = loadTaggedKnowledgeIds(masteries);
        Map<Long, Integer> mistakeCounts = countMistakesByKnowledge(mistakes, false);
        Map<Long, Integer> dueReviewCounts = countMistakesByKnowledge(mistakes, true);
        Map<Long, Integer> recentWrongCounts = countRecentWrongByKnowledge(records);

        LearningAnalyticsResponse response = new LearningAnalyticsResponse();
        response.setOverallStats(buildOverallStats(records, masteries, mistakes, completedPractices, taggedKnowledgeIds));
        response.setKnowledgeCoverage(buildKnowledgeCoverage(masteries, taggedKnowledgeIds));
        response.setKnowledgeHeatmap(buildKnowledgeHeatmap(masteries, knowledgeById, subjectById, mistakeCounts, dueReviewCounts));
        response.setWeakPoints(buildWeakPoints(masteries, knowledgeById, mistakeCounts, dueReviewCounts, recentWrongCounts));
        response.setMistakeTypeStats(buildMistakeTypeStats(mistakes));
        response.setLearningTrend(buildLearningTrend(records));
        response.setDiagnosis(buildDiagnosis(response.getOverallStats(), response.getKnowledgeCoverage(),
                response.getWeakPoints(), response.getKnowledgeHeatmap()));
        response.setRecommendations(buildRecommendations(response.getOverallStats(), response.getKnowledgeCoverage(),
                response.getWeakPoints()));
        return response;
    }

    /**
     * 获取周报/月报。周报统计近7天，月报统计近30天。
     */
    public Map<String, Object> getPeriodReport(Long userId, String period) {
        boolean monthly = "month".equalsIgnoreCase(period) || "monthly".equalsIgnoreCase(period);
        int days = monthly ? 30 : 7;
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getUserId, userId)
                        .ge(AnswerRecord::getCreatedAt, start)
                        .lt(AnswerRecord::getCreatedAt, end)
                        .orderByAsc(AnswerRecord::getCreatedAt)
        );
        List<MistakeBook> mistakes = mistakeBookMapper.selectList(
                new LambdaQueryWrapper<MistakeBook>()
                        .eq(MistakeBook::getUserId, userId)
        );
        List<MistakeBook> newMistakes = mistakes.stream()
                .filter(mistake -> mistake.getCreatedAt() != null
                        && !mistake.getCreatedAt().isBefore(start)
                        && mistake.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());
        List<PracticeRecord> completedPractices = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, 1)
                        .ge(PracticeRecord::getEndTime, start)
                        .lt(PracticeRecord::getEndTime, end)
        );
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .orderByAsc(KnowledgeMastery::getMasteryLevel)
        );

        long correct = records.stream().filter(this::isCorrect).count();
        int studyMinutes = completedPractices.stream()
                .map(PracticeRecord::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (studyMinutes > 0) {
            studyMinutes = Math.max(1, (int) Math.ceil(studyMinutes / 60.0));
        } else {
            studyMinutes = records.size() * 5;
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", monthly ? "month" : "week");
        report.put("periodName", monthly ? "近30天学习月报" : "近7天学习周报");
        report.put("startDate", startDate);
        report.put("endDate", today);
        report.put("questionCount", records.size());
        report.put("correctCount", correct);
        report.put("accuracyRate", percent(correct, records.size()));
        report.put("studyMinutes", studyMinutes);
        report.put("practiceCount", completedPractices.size());
        report.put("newMistakeCount", newMistakes.size());
        report.put("dueReviewCount", mistakes.stream().filter(this::isDueReview).count());
        report.put("trend", buildPeriodTrend(records, days));
        report.put("weakPoints", buildPeriodWeakPoints(records));
        report.put("improvedKnowledge", buildImprovedKnowledge(masteries, start));
        report.put("mistakeTypeStats", buildMistakeTypeStats(newMistakes));
        report.put("summary", buildPeriodSummary(monthly, records.size(), correct, newMistakes.size(), masteries));
        report.put("actions", buildPeriodActions(records, newMistakes, masteries));
        return report;
    }

    private LearningAnalyticsResponse.OverallStats buildOverallStats(List<AnswerRecord> records,
                                                                     List<KnowledgeMastery> masteries,
                                                                     List<MistakeBook> mistakes,
                                                                     List<PracticeRecord> completedPractices,
                                                                     Set<Long> taggedKnowledgeIds) {
        LearningAnalyticsResponse.OverallStats stats = new LearningAnalyticsResponse.OverallStats();

        long totalQuestions = records.size();
        long correctCount = records.stream().filter(this::isCorrect).count();
        int totalDurationSeconds = completedPractices.stream()
                .map(PracticeRecord::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        stats.setTotalQuestions((int) totalQuestions);
        stats.setCorrectCount((int) correctCount);
        stats.setAccuracyRate(percent(correctCount, totalQuestions));
        stats.setRecentAccuracyRate(calculateRecentAccuracy(records));
        stats.setTotalStudyTime(totalDurationSeconds > 0
                ? Math.max(1, (int) Math.ceil(totalDurationSeconds / 60.0))
                : (int) totalQuestions * 5);
        stats.setStreakDays(calculateStreakDays(records));
        stats.setActiveDays(calculateActiveDays(records, completedPractices));
        stats.setMistakeCount(mistakes.size());
        stats.setDueReviewCount((int) mistakes.stream().filter(this::isDueReview).count());
        stats.setTotalKnowledgeCount(taggedKnowledgeIds.size());
        stats.setMasteredKnowledgeCount((int) masteries.stream().filter(m -> masteryValue(m) >= 80).count());
        stats.setWeakKnowledgeCount((int) masteries.stream().filter(m -> masteryValue(m) < 60).count());
        return stats;
    }

    private LearningAnalyticsResponse.KnowledgeCoverage buildKnowledgeCoverage(List<KnowledgeMastery> masteries,
                                                                               Set<Long> taggedKnowledgeIds) {
        Set<Long> practicedKnowledgeIds = masteries.stream()
                .map(KnowledgeMastery::getKnowledgeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        int mastered = (int) masteries.stream().filter(m -> masteryValue(m) >= 80).count();
        int weak = (int) masteries.stream().filter(m -> masteryValue(m) < 60).count();

        LearningAnalyticsResponse.KnowledgeCoverage coverage = new LearningAnalyticsResponse.KnowledgeCoverage();
        coverage.setTotalTaggedKnowledge(taggedKnowledgeIds.size());
        coverage.setPracticedKnowledge(practicedKnowledgeIds.size());
        coverage.setMasteredKnowledge(mastered);
        coverage.setWeakKnowledge(weak);
        coverage.setUnpracticedKnowledge(Math.max(0, taggedKnowledgeIds.size() - practicedKnowledgeIds.size()));
        coverage.setCoverageRate(percent(practicedKnowledgeIds.size(), taggedKnowledgeIds.size()));
        return coverage;
    }

    private List<LearningAnalyticsResponse.KnowledgeHeatmap> buildKnowledgeHeatmap(List<KnowledgeMastery> masteries,
                                                                                   Map<Long, KnowledgePoint> knowledgeById,
                                                                                   Map<Long, Subject> subjectById,
                                                                                   Map<Long, Integer> mistakeCounts,
                                                                                   Map<Long, Integer> dueReviewCounts) {
        return masteries.stream().map(mastery -> {
            int total = Optional.ofNullable(mastery.getTotalQuestions()).orElse(0);
            int correct = Optional.ofNullable(mastery.getCorrectQuestions()).orElse(0);
            int wrong = Math.max(0, total - correct);
            int mistakeCount = mistakeCounts.getOrDefault(mastery.getKnowledgeId(), 0);
            int dueReviewCount = dueReviewCounts.getOrDefault(mastery.getKnowledgeId(), 0);
            BigDecimal masteryLevel = normalize(mastery.getMasteryLevel());
            KnowledgePoint knowledgePoint = knowledgeById.get(mastery.getKnowledgeId());
            Long subjectId = knowledgePoint == null ? null : knowledgePoint.getSubjectId();
            Subject subject = subjectId == null ? null : subjectById.get(subjectId);

            LearningAnalyticsResponse.KnowledgeHeatmap item = new LearningAnalyticsResponse.KnowledgeHeatmap();
            item.setKnowledgeId(mastery.getKnowledgeId());
            item.setKnowledgeName(getKnowledgeName(knowledgeById, mastery.getKnowledgeId()));
            item.setSubjectId(subjectId);
            item.setSubjectName(subject == null ? "未分类" : subject.getName());
            item.setMasteryLevel(masteryLevel);
            item.setAccuracyRate(percent(correct, total));
            item.setTotalQuestions(total);
            item.setCorrectQuestions(correct);
            item.setWrongQuestions(wrong);
            item.setMistakeCount(mistakeCount);
            item.setDueReviewCount(dueReviewCount);
            item.setStatus(getMasteryStatus(masteryLevel));
            item.setRiskLevel(getRiskLevel(masteryLevel, mistakeCount, dueReviewCount));
            item.setLastPracticeAt(mastery.getLastPracticeAt());
            return item;
        }).sorted(Comparator
                .comparing((LearningAnalyticsResponse.KnowledgeHeatmap item) -> riskRank(item.getRiskLevel()))
                .thenComparing(LearningAnalyticsResponse.KnowledgeHeatmap::getMasteryLevel))
                .collect(Collectors.toList());
    }

    private List<LearningAnalyticsResponse.WeakPointAnalysis> buildWeakPoints(List<KnowledgeMastery> masteries,
                                                                              Map<Long, KnowledgePoint> knowledgeById,
                                                                              Map<Long, Integer> mistakeCounts,
                                                                              Map<Long, Integer> dueReviewCounts,
                                                                              Map<Long, Integer> recentWrongCounts) {
        return masteries.stream()
                .filter(mastery -> masteryValue(mastery) < 80
                        || mistakeCounts.getOrDefault(mastery.getKnowledgeId(), 0) > 0
                        || dueReviewCounts.getOrDefault(mastery.getKnowledgeId(), 0) > 0)
                .sorted(Comparator.comparing((KnowledgeMastery mastery) ->
                        priorityScore(mastery, mistakeCounts, dueReviewCounts, recentWrongCounts)).reversed())
                .limit(6)
                .map(mastery -> {
                    int total = Optional.ofNullable(mastery.getTotalQuestions()).orElse(0);
                    int correct = Optional.ofNullable(mastery.getCorrectQuestions()).orElse(0);
                    int wrong = Math.max(0, total - correct);
                    int mistakeCount = mistakeCounts.getOrDefault(mastery.getKnowledgeId(), 0);
                    int recentWrongCount = recentWrongCounts.getOrDefault(mastery.getKnowledgeId(), 0);
                    int dueReviewCount = dueReviewCounts.getOrDefault(mastery.getKnowledgeId(), 0);
                    BigDecimal masteryLevel = normalize(mastery.getMasteryLevel());

                    LearningAnalyticsResponse.WeakPointAnalysis analysis = new LearningAnalyticsResponse.WeakPointAnalysis();
                    analysis.setKnowledgeId(mastery.getKnowledgeId());
                    analysis.setKnowledgeName(getKnowledgeName(knowledgeById, mastery.getKnowledgeId()));
                    analysis.setMasteryLevel(masteryLevel);
                    analysis.setTotalQuestions(total);
                    analysis.setCorrectQuestions(correct);
                    analysis.setWrongQuestions(wrong);
                    analysis.setMistakeCount(mistakeCount);
                    analysis.setRecentWrongCount(recentWrongCount);
                    analysis.setDueReviewCount(dueReviewCount);
                    analysis.setWeaknessType(inferWeaknessType(masteryLevel, mistakeCount, recentWrongCount));
                    analysis.setPriorityScore(BigDecimal.valueOf(priorityScore(mastery, mistakeCounts,
                            dueReviewCounts, recentWrongCounts)).setScale(1, RoundingMode.HALF_UP));
                    analysis.setEvidence(buildWeakPointEvidence(total, wrong, mistakeCount, recentWrongCount, dueReviewCount));
                    analysis.setSuggestion(generateSuggestion(analysis));
                    analysis.setRelatedQuestionIds(findRelatedQuestionIds(mastery.getKnowledgeId(), 5));
                    return analysis;
                })
                .collect(Collectors.toList());
    }

    private List<LearningAnalyticsResponse.MistakeTypeStats> buildMistakeTypeStats(List<MistakeBook> mistakes) {
        Map<String, Long> typeCount = mistakes.stream()
                .collect(Collectors.groupingBy(
                        mistake -> normalizeMistakeType(mistake.getMistakeType()),
                        Collectors.counting()
                ));

        long total = mistakes.size();
        return typeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    LearningAnalyticsResponse.MistakeTypeStats stats = new LearningAnalyticsResponse.MistakeTypeStats();
                    stats.setMistakeType(entry.getKey());
                    stats.setLabel(getMistakeTypeLabel(entry.getKey()));
                    stats.setCount(entry.getValue().intValue());
                    stats.setPercentage(percent(entry.getValue(), total));
                    stats.setSuggestion(getMistakeTypeSuggestion(entry.getKey()));
                    return stats;
                })
                .collect(Collectors.toList());
    }

    private LearningAnalyticsResponse.LearningTrend buildLearningTrend(List<AnswerRecord> records) {
        LearningAnalyticsResponse.LearningTrend trend = new LearningAnalyticsResponse.LearningTrend();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        Map<LocalDate, List<AnswerRecord>> recordsByDate = records.stream()
                .filter(record -> record.getCreatedAt() != null)
                .collect(Collectors.groupingBy(record -> record.getCreatedAt().toLocalDate()));

        List<String> dates = new ArrayList<>();
        List<Integer> questionCounts = new ArrayList<>();
        List<Integer> correctCounts = new ArrayList<>();
        List<BigDecimal> accuracyRates = new ArrayList<>();

        for (int i = RECENT_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<AnswerRecord> dailyRecords = recordsByDate.getOrDefault(date, List.of());
            long correct = dailyRecords.stream().filter(this::isCorrect).count();

            dates.add(date.format(formatter));
            questionCounts.add(dailyRecords.size());
            correctCounts.add((int) correct);
            accuracyRates.add(percent(correct, dailyRecords.size()));
        }

        trend.setDates(dates);
        trend.setQuestionCounts(questionCounts);
        trend.setCorrectCounts(correctCounts);
        trend.setAccuracyRates(accuracyRates);
        return trend;
    }

    private LearningAnalyticsResponse.LearningTrend buildPeriodTrend(List<AnswerRecord> records, int days) {
        LearningAnalyticsResponse.LearningTrend trend = new LearningAnalyticsResponse.LearningTrend();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        Map<LocalDate, List<AnswerRecord>> recordsByDate = records.stream()
                .filter(record -> record.getCreatedAt() != null)
                .collect(Collectors.groupingBy(record -> record.getCreatedAt().toLocalDate()));

        List<String> dates = new ArrayList<>();
        List<Integer> questionCounts = new ArrayList<>();
        List<Integer> correctCounts = new ArrayList<>();
        List<BigDecimal> accuracyRates = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<AnswerRecord> daily = recordsByDate.getOrDefault(date, List.of());
            long correct = daily.stream().filter(this::isCorrect).count();
            dates.add(date.format(formatter));
            questionCounts.add(daily.size());
            correctCounts.add((int) correct);
            accuracyRates.add(percent(correct, daily.size()));
        }

        trend.setDates(dates);
        trend.setQuestionCounts(questionCounts);
        trend.setCorrectCounts(correctCounts);
        trend.setAccuracyRates(accuracyRates);
        return trend;
    }

    private List<Map<String, Object>> buildPeriodWeakPoints(List<AnswerRecord> records) {
        Map<Long, int[]> statsByKnowledge = new HashMap<>();
        for (AnswerRecord record : records) {
            Question question = questionMapper.selectById(record.getQuestionId());
            for (Long knowledgeId : knowledgeTaggingService.resolveKnowledgeIds(question)) {
                int[] stats = statsByKnowledge.computeIfAbsent(knowledgeId, ignored -> new int[2]);
                stats[0]++;
                if (isCorrect(record)) {
                    stats[1]++;
                }
            }
        }

        return statsByKnowledge.entrySet().stream()
                .filter(entry -> entry.getValue()[0] > entry.getValue()[1])
                .sorted(Comparator
                        .comparing((Map.Entry<Long, int[]> entry) -> entry.getValue()[0] - entry.getValue()[1])
                        .reversed())
                .limit(5)
                .map(entry -> {
                    int total = entry.getValue()[0];
                    int correct = entry.getValue()[1];
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("knowledgeId", entry.getKey());
                    item.put("knowledgeName", getKnowledgeName(new HashMap<>(), entry.getKey()));
                    item.put("questionCount", total);
                    item.put("wrongCount", total - correct);
                    item.put("accuracyRate", percent(correct, total));
                    item.put("suggestion", "先复盘本周期错题，再补做同知识点基础题。");
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildImprovedKnowledge(List<KnowledgeMastery> masteries, LocalDateTime start) {
        return masteries.stream()
                .filter(mastery -> mastery.getLastPracticeAt() != null && !mastery.getLastPracticeAt().isBefore(start))
                .filter(mastery -> masteryValue(mastery) >= 60)
                .sorted(Comparator.comparing(KnowledgeMastery::getMasteryLevel).reversed())
                .limit(5)
                .map(mastery -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("knowledgeId", mastery.getKnowledgeId());
                    item.put("knowledgeName", getKnowledgeName(new HashMap<>(), mastery.getKnowledgeId()));
                    item.put("masteryLevel", normalize(mastery.getMasteryLevel()));
                    item.put("totalQuestions", Optional.ofNullable(mastery.getTotalQuestions()).orElse(0));
                    item.put("correctQuestions", Optional.ofNullable(mastery.getCorrectQuestions()).orElse(0));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private String buildPeriodSummary(boolean monthly,
                                      int questionCount,
                                      long correctCount,
                                      int newMistakeCount,
                                      List<KnowledgeMastery> masteries) {
        if (questionCount == 0) {
            return (monthly ? "近30天" : "近7天") + "还没有答题记录，建议先完成一组专项练习形成报告数据。";
        }
        String topWeak = masteries.stream()
                .filter(mastery -> masteryValue(mastery) < 80)
                .min(Comparator.comparingDouble(this::masteryValue))
                .map(mastery -> getKnowledgeName(new HashMap<>(), mastery.getKnowledgeId()))
                .orElse("暂无明显薄弱点");
        return (monthly ? "近30天" : "近7天") + "完成 " + questionCount + " 道题，正确率 "
                + percent(correctCount, questionCount) + "%，新增错题 " + newMistakeCount
                + " 道。下一步建议重点关注：" + topWeak + "。";
    }

    private List<String> buildPeriodActions(List<AnswerRecord> records,
                                            List<MistakeBook> newMistakes,
                                            List<KnowledgeMastery> masteries) {
        List<String> actions = new ArrayList<>();
        long dueReview = newMistakes.stream().filter(this::isDueReview).count();
        if (dueReview > 0) {
            actions.add("先完成 " + dueReview + " 道到期错题复盘。");
        }
        masteries.stream()
                .filter(mastery -> masteryValue(mastery) < 75)
                .sorted(Comparator.comparingDouble(this::masteryValue))
                .limit(2)
                .forEach(mastery -> actions.add("针对 " + getKnowledgeName(new HashMap<>(), mastery.getKnowledgeId())
                        + " 做一组知识点专项练习。"));
        if (records.size() < 10) {
            actions.add("本周期答题样本偏少，先补到 10 道以上让诊断更稳。");
        }
        if (actions.isEmpty()) {
            actions.add("保持当前节奏，下一轮增加限时综合练习。");
        }
        return actions;
    }

    private LearningAnalyticsResponse.LearningDiagnosis buildDiagnosis(LearningAnalyticsResponse.OverallStats stats,
                                                                       LearningAnalyticsResponse.KnowledgeCoverage coverage,
                                                                       List<LearningAnalyticsResponse.WeakPointAnalysis> weakPoints,
                                                                       List<LearningAnalyticsResponse.KnowledgeHeatmap> heatmap) {
        LearningAnalyticsResponse.LearningDiagnosis diagnosis = new LearningAnalyticsResponse.LearningDiagnosis();
        String strongestPoint = heatmap.stream()
                .max(Comparator.comparing(LearningAnalyticsResponse.KnowledgeHeatmap::getMasteryLevel))
                .map(LearningAnalyticsResponse.KnowledgeHeatmap::getKnowledgeName)
                .orElse("暂无");
        String weakestPoint = weakPoints.stream()
                .findFirst()
                .map(LearningAnalyticsResponse.WeakPointAnalysis::getKnowledgeName)
                .orElse("暂无明显薄弱点");

        diagnosis.setLearningStage(inferLearningStage(stats));
        diagnosis.setLevel(inferLearningLevel(stats));
        diagnosis.setStrongestPoint(strongestPoint);
        diagnosis.setWeakestPoint(weakestPoint);
        diagnosis.setDataConfidence(inferDataConfidence(stats, coverage));
        diagnosis.setPrimaryIssue(inferPrimaryIssue(stats, coverage, weakPoints));
        diagnosis.setNextFocus(inferNextFocus(stats, weakPoints));
        diagnosis.setSummary(buildSummary(stats, coverage, weakestPoint));
        diagnosis.setEvidence(List.of(
                "累计答题 " + stats.getTotalQuestions() + " 道，正确率 " + stats.getAccuracyRate() + "%",
                "近 " + RECENT_DAYS + " 天正确率 " + stats.getRecentAccuracyRate() + "%，连续学习 " + stats.getStreakDays() + " 天",
                "已覆盖 " + coverage.getPracticedKnowledge() + "/" + coverage.getTotalTaggedKnowledge()
                        + " 个已标注知识点，覆盖率 " + coverage.getCoverageRate() + "%",
                "当前错题 " + stats.getMistakeCount() + " 道，其中到期复盘 " + stats.getDueReviewCount() + " 道"
        ));
        return diagnosis;
    }

    private List<LearningAnalyticsResponse.ActionRecommendation> buildRecommendations(
            LearningAnalyticsResponse.OverallStats stats,
            LearningAnalyticsResponse.KnowledgeCoverage coverage,
            List<LearningAnalyticsResponse.WeakPointAnalysis> weakPoints) {
        List<LearningAnalyticsResponse.ActionRecommendation> recommendations = new ArrayList<>();

        if (stats.getDueReviewCount() > 0) {
            recommendations.add(buildRecommendation(
                    "REVIEW",
                    "先清到期错题",
                    "今天有 " + stats.getDueReviewCount() + " 道错题需要复盘，优先处理能降低重复失误。",
                    null,
                    null,
                    1,
                    Math.min(40, Math.max(10, stats.getDueReviewCount() * 5)),
                    "进入错题本，按到期时间复盘"
            ));
        }

        weakPoints.stream().limit(2).forEach(weak -> recommendations.add(buildRecommendation(
                "KNOWLEDGE_PRACTICE",
                weak.getKnowledgeName() + "专项补强",
                weak.getEvidence() + "。建议先做基础题确认概念，再做中档题巩固方法。",
                weak.getKnowledgeId(),
                weak.getKnowledgeName(),
                recommendations.size() + 1,
                25,
                "按知识点发起专项练习"
        )));

        if (coverage.getTotalTaggedKnowledge() > 0 && coverage.getCoverageRate().compareTo(BigDecimal.valueOf(70)) < 0) {
            recommendations.add(buildRecommendation(
                    "COVERAGE",
                    "补齐诊断覆盖",
                    "还有 " + coverage.getUnpracticedKnowledge() + " 个已标注知识点没有形成掌握度记录。",
                    null,
                    null,
                    recommendations.size() + 1,
                    20,
                    "选择未练知识点做诊断题"
            ));
        }

        if (recommendations.isEmpty()) {
            recommendations.add(buildRecommendation(
                    "CHALLENGE",
                    "保持综合训练",
                    "目前没有明显高风险知识点，可以用综合题保持手感并冲击高阶题。",
                    null,
                    null,
                    1,
                    30,
                    "完成一组中高难度综合练习"
            ));
        }

        return recommendations;
    }

    private LearningAnalyticsResponse.ActionRecommendation buildRecommendation(String type,
                                                                               String title,
                                                                               String description,
                                                                               Long knowledgeId,
                                                                               String knowledgeName,
                                                                               int priority,
                                                                               int estimatedMinutes,
                                                                               String action) {
        LearningAnalyticsResponse.ActionRecommendation recommendation = new LearningAnalyticsResponse.ActionRecommendation();
        recommendation.setType(type);
        recommendation.setTitle(title);
        recommendation.setDescription(description);
        recommendation.setKnowledgeId(knowledgeId);
        recommendation.setKnowledgeName(knowledgeName);
        recommendation.setPriority(priority);
        recommendation.setEstimatedMinutes(estimatedMinutes);
        recommendation.setAction(action);
        return recommendation;
    }

    private Map<Long, KnowledgePoint> loadKnowledgeById(List<KnowledgeMastery> masteries) {
        Set<Long> ids = masteries.stream()
                .map(KnowledgeMastery::getKnowledgeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ids.isEmpty()) {
            return new HashMap<>();
        }

        return knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>().in(KnowledgePoint::getId, ids)
        ).stream().collect(Collectors.toMap(
                KnowledgePoint::getId,
                point -> point,
                (first, ignored) -> first,
                LinkedHashMap::new
        ));
    }

    private Map<Long, Subject> loadSubjectsByKnowledge(Map<Long, KnowledgePoint> knowledgeById) {
        Set<Long> subjectIds = knowledgeById.values().stream()
                .map(KnowledgePoint::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (subjectIds.isEmpty()) {
            return new HashMap<>();
        }

        return subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().in(Subject::getId, subjectIds)
        ).stream().collect(Collectors.toMap(
                Subject::getId,
                subject -> subject,
                (first, ignored) -> first,
                LinkedHashMap::new
        ));
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

    private Map<Long, Integer> countMistakesByKnowledge(List<MistakeBook> mistakes, boolean onlyDue) {
        Map<Long, Integer> counts = new HashMap<>();
        for (MistakeBook mistake : mistakes) {
            if (onlyDue && !isDueReview(mistake)) {
                continue;
            }
            Question question = questionMapper.selectById(mistake.getQuestionId());
            for (Long knowledgeId : knowledgeTaggingService.resolveKnowledgeIds(question)) {
                counts.merge(knowledgeId, 1, Integer::sum);
            }
        }
        return counts;
    }

    private Map<Long, Integer> countRecentWrongByKnowledge(List<AnswerRecord> records) {
        LocalDateTime start = LocalDate.now().minusDays(RECENT_DAYS - 1L).atStartOfDay();
        Map<Long, Integer> counts = new HashMap<>();
        for (AnswerRecord record : records) {
            if (record.getCreatedAt() == null || record.getCreatedAt().isBefore(start) || isCorrect(record)) {
                continue;
            }
            Question question = questionMapper.selectById(record.getQuestionId());
            for (Long knowledgeId : knowledgeTaggingService.resolveKnowledgeIds(question)) {
                counts.merge(knowledgeId, 1, Integer::sum);
            }
        }
        return counts;
    }

    private List<Long> findRelatedQuestionIds(Long knowledgeId, int limit) {
        if (knowledgeId == null) {
            return List.of();
        }
        return questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .apply("FIND_IN_SET({0}, knowledge_ids)", knowledgeId)
                        .orderByAsc(Question::getDifficulty)
                        .last("LIMIT " + limit)
        ).stream().map(Question::getId).collect(Collectors.toList());
    }

    private BigDecimal calculateRecentAccuracy(List<AnswerRecord> records) {
        LocalDateTime start = LocalDate.now().minusDays(RECENT_DAYS - 1L).atStartOfDay();
        List<AnswerRecord> recentRecords = records.stream()
                .filter(record -> record.getCreatedAt() != null && !record.getCreatedAt().isBefore(start))
                .collect(Collectors.toList());
        long correct = recentRecords.stream().filter(this::isCorrect).count();
        return percent(correct, recentRecords.size());
    }

    private int calculateStreakDays(List<AnswerRecord> records) {
        Set<LocalDate> activeDates = records.stream()
                .map(AnswerRecord::getCreatedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate date = LocalDate.now();
        while (activeDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
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

    private String inferLearningStage(LearningAnalyticsResponse.OverallStats stats) {
        if (stats.getTotalQuestions() < 5) {
            return "起步诊断期";
        }
        if (stats.getAccuracyRate().compareTo(BigDecimal.valueOf(85)) >= 0 && stats.getWeakKnowledgeCount() == 0) {
            return "拓展提升期";
        }
        if (stats.getAccuracyRate().compareTo(BigDecimal.valueOf(65)) >= 0) {
            return "巩固修复期";
        }
        return "基础补强期";
    }

    private String inferLearningLevel(LearningAnalyticsResponse.OverallStats stats) {
        if (stats.getAccuracyRate().compareTo(BigDecimal.valueOf(85)) >= 0) {
            return "优秀";
        }
        if (stats.getAccuracyRate().compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "良好";
        }
        if (stats.getAccuracyRate().compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "待提升";
        }
        return "需补基础";
    }

    private String inferDataConfidence(LearningAnalyticsResponse.OverallStats stats,
                                       LearningAnalyticsResponse.KnowledgeCoverage coverage) {
        if (stats.getTotalQuestions() < 5 || coverage.getPracticedKnowledge() < 2) {
            return "推荐依据：样本较少，先完成一轮诊断练习后推荐会更准";
        }
        if (stats.getTotalQuestions() < 20 || coverage.getCoverageRate().compareTo(BigDecimal.valueOf(60)) < 0) {
            return "推荐依据：已有参考记录，继续补齐知识点覆盖后推荐会更准";
        }
        return "推荐依据：数据较充分，已基于 " + stats.getTotalQuestions() + " 道答题记录和 "
                + coverage.getCoverageRate() + "% 知识点覆盖生成";
    }

    private String inferPrimaryIssue(LearningAnalyticsResponse.OverallStats stats,
                                     LearningAnalyticsResponse.KnowledgeCoverage coverage,
                                     List<LearningAnalyticsResponse.WeakPointAnalysis> weakPoints) {
        if (stats.getDueReviewCount() > 0) {
            return "错题复盘滞后";
        }
        if (!weakPoints.isEmpty() && weakPoints.get(0).getMasteryLevel().compareTo(BigDecimal.valueOf(50)) < 0) {
            return "核心知识点掌握不足";
        }
        if (coverage.getCoverageRate().compareTo(BigDecimal.valueOf(60)) < 0) {
            return "诊断覆盖不足";
        }
        if (stats.getRecentAccuracyRate().compareTo(stats.getAccuracyRate().subtract(BigDecimal.TEN)) < 0) {
            return "近期正确率下滑";
        }
        return "保持训练节奏";
    }

    private String inferNextFocus(LearningAnalyticsResponse.OverallStats stats,
                                  List<LearningAnalyticsResponse.WeakPointAnalysis> weakPoints) {
        if (stats.getDueReviewCount() > 0) {
            return "先完成今日到期错题复盘";
        }
        return weakPoints.stream()
                .findFirst()
                .map(weak -> "优先补强 " + weak.getKnowledgeName())
                .orElse("进行一组综合提升练习");
    }

    private String buildSummary(LearningAnalyticsResponse.OverallStats stats,
                                LearningAnalyticsResponse.KnowledgeCoverage coverage,
                                String weakestPoint) {
        if (stats.getTotalQuestions() == 0) {
            return "当前还没有答题记录，建议先完成一组诊断练习，系统才能形成可靠画像。";
        }
        return "当前累计答题 " + stats.getTotalQuestions() + " 道，整体正确率 "
                + stats.getAccuracyRate() + "%，知识点覆盖率 " + coverage.getCoverageRate()
                + "%。下一步重点关注：" + weakestPoint + "。";
    }

    private String inferWeaknessType(BigDecimal masteryLevel, int mistakeCount, int recentWrongCount) {
        if (masteryLevel.compareTo(BigDecimal.valueOf(35)) < 0) {
            return "概念断点";
        }
        if (recentWrongCount >= 2) {
            return "近期反复失误";
        }
        if (mistakeCount >= 2) {
            return "错题未巩固";
        }
        if (masteryLevel.compareTo(BigDecimal.valueOf(65)) < 0) {
            return "方法不稳";
        }
        return "熟练度不足";
    }

    private String buildWeakPointEvidence(int total, int wrong, int mistakeCount, int recentWrongCount, int dueReviewCount) {
        List<String> evidence = new ArrayList<>();
        evidence.add("已练 " + total + " 题，错 " + wrong + " 题");
        if (mistakeCount > 0) {
            evidence.add("错题本 " + mistakeCount + " 道");
        }
        if (recentWrongCount > 0) {
            evidence.add("近7天错 " + recentWrongCount + " 次");
        }
        if (dueReviewCount > 0) {
            evidence.add("到期复盘 " + dueReviewCount + " 道");
        }
        return String.join("，", evidence);
    }

    private String generateSuggestion(LearningAnalyticsResponse.WeakPointAnalysis weak) {
        if ("概念断点".equals(weak.getWeaknessType())) {
            return "先回看定义、公式来源和典型例题，再做 5 道基础题确认概念。";
        }
        if ("近期反复失误".equals(weak.getWeaknessType())) {
            return "把近 7 天错题按错因重做一遍，重点记录审题条件和关键转化步骤。";
        }
        if ("错题未巩固".equals(weak.getWeaknessType())) {
            return "优先复盘错题本中同知识点题目，答对后再追加 3 道变式题。";
        }
        if ("方法不稳".equals(weak.getWeaknessType())) {
            return "整理该知识点常用解题模板，按基础题到中档题逐步提高难度。";
        }
        return "掌握基础已经具备，建议用限时训练提升稳定性和速度。";
    }

    private double priorityScore(KnowledgeMastery mastery,
                                 Map<Long, Integer> mistakeCounts,
                                 Map<Long, Integer> dueReviewCounts,
                                 Map<Long, Integer> recentWrongCounts) {
        Long knowledgeId = mastery.getKnowledgeId();
        return (100 - masteryValue(mastery))
                + mistakeCounts.getOrDefault(knowledgeId, 0) * 8.0
                + dueReviewCounts.getOrDefault(knowledgeId, 0) * 12.0
                + recentWrongCounts.getOrDefault(knowledgeId, 0) * 10.0;
    }

    private String getMasteryStatus(BigDecimal masteryLevel) {
        if (masteryLevel.compareTo(BigDecimal.valueOf(30)) < 0) {
            return "待补基础";
        }
        if (masteryLevel.compareTo(BigDecimal.valueOf(60)) < 0) {
            return "薄弱";
        }
        if (masteryLevel.compareTo(BigDecimal.valueOf(80)) < 0) {
            return "巩固中";
        }
        return "稳定掌握";
    }

    private String getRiskLevel(BigDecimal masteryLevel, int mistakeCount, int dueReviewCount) {
        if (masteryLevel.compareTo(BigDecimal.valueOf(50)) < 0 || dueReviewCount > 0) {
            return "HIGH";
        }
        if (masteryLevel.compareTo(BigDecimal.valueOf(75)) < 0 || mistakeCount > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private int riskRank(String riskLevel) {
        if ("HIGH".equals(riskLevel)) {
            return 0;
        }
        if ("MEDIUM".equals(riskLevel)) {
            return 1;
        }
        return 2;
    }

    private boolean isCorrect(AnswerRecord record) {
        return record != null && Integer.valueOf(1).equals(record.getIsCorrect());
    }

    private boolean isDueReview(MistakeBook mistake) {
        return mistake.getNextReviewDate() != null && !mistake.getNextReviewDate().isAfter(LocalDate.now());
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(1, RoundingMode.HALF_UP);
    }

    private double masteryValue(KnowledgeMastery mastery) {
        return Optional.ofNullable(mastery.getMasteryLevel())
                .orElse(BigDecimal.ZERO)
                .doubleValue();
    }

    private String getKnowledgeName(Map<Long, KnowledgePoint> knowledgeById, Long knowledgeId) {
        KnowledgePoint point = knowledgeById.get(knowledgeId);
        if (point == null && knowledgeId != null) {
            point = knowledgePointMapper.selectById(knowledgeId);
        }
        return point != null ? point.getName() : "知识点" + knowledgeId;
    }

    private Set<Long> parseKnowledgeIds(String knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isBlank()) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String value : knowledgeIds.split(",")) {
            try {
                ids.add(Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private String normalizeMistakeType(String mistakeType) {
        return mistakeType == null || mistakeType.isBlank() ? "UNKNOWN" : mistakeType;
    }

    private String getMistakeTypeLabel(String mistakeType) {
        return switch (mistakeType) {
            case "CONCEPT_ERROR", "KNOWLEDGE_GAP" -> "概念理解错误";
            case "CARELESS" -> "审题/计算粗心";
            case "WRONG_APPROACH" -> "解题方法错误";
            case "INCOMPLETE" -> "步骤不完整";
            case "NEEDS_REVIEW" -> "需要复盘巩固";
            default -> "未分类错因";
        };
    }

    private String getMistakeTypeSuggestion(String mistakeType) {
        return switch (mistakeType) {
            case "CONCEPT_ERROR", "KNOWLEDGE_GAP" -> "回到定义和公式来源，先补概念再刷题。";
            case "CARELESS" -> "建立审题圈画、代入检验和结果回看清单。";
            case "WRONG_APPROACH" -> "对比标准解法，总结题型入口和关键转化。";
            case "INCOMPLETE" -> "按评分点补全步骤，练习规范表达。";
            case "NEEDS_REVIEW" -> "按到期时间复盘，答对后做一道变式题。";
            default -> "补充错因备注，方便后续生成更准确的复习策略。";
        };
    }
}
