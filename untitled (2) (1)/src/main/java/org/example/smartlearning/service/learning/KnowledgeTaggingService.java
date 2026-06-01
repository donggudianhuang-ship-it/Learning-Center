package org.example.smartlearning.service.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.entity.KnowledgePoint;
import org.example.smartlearning.entity.Question;
import org.example.smartlearning.entity.Subject;
import org.example.smartlearning.mapper.KnowledgePointMapper;
import org.example.smartlearning.mapper.QuestionMapper;
import org.example.smartlearning.mapper.SubjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目知识点标注服务。
 * 已有标注优先；缺失标注时仅对数学题做保守关键词推断。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeTaggingService {

    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final SubjectMapper subjectMapper;

    @Transactional
    public List<Long> resolveKnowledgeIds(Question question) {
        if (question == null) {
            return Collections.emptyList();
        }

        List<Long> existingIds = parseKnowledgeIds(question.getKnowledgeIds());
        if (!existingIds.isEmpty()) {
            return existingIds;
        }

        List<Long> inferredIds = inferMathKnowledgeIds(question);
        if (!inferredIds.isEmpty()) {
            question.setKnowledgeIds(inferredIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            questionMapper.updateById(question);
        }
        return inferredIds;
    }

    public List<String> resolveKnowledgeNames(Question question) {
        return resolveKnowledgeIds(question).stream()
                .map(knowledgePointMapper::selectById)
                .filter(Objects::nonNull)
                .map(KnowledgePoint::getName)
                .collect(Collectors.toList());
    }

    private List<Long> parseKnowledgeIds(String knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isBlank()) {
            return Collections.emptyList();
        }

        List<Long> ids = new ArrayList<>();
        for (String id : knowledgeIds.split(",")) {
            try {
                ids.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private List<Long> inferMathKnowledgeIds(Question question) {
        Subject subject = subjectMapper.selectById(question.getSubjectId());
        if (subject == null || !"数学".equals(subject.getName())) {
            return Collections.emptyList();
        }

        Map<String, Long> knowledgeIdsByName = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getSubjectId, subject.getId())
        ).stream().collect(Collectors.toMap(
                KnowledgePoint::getName,
                KnowledgePoint::getId,
                (first, ignored) -> first,
                LinkedHashMap::new
        ));

        String text = String.join(" ",
                Optional.ofNullable(question.getContent()).orElse(""),
                Optional.ofNullable(question.getAnalysis()).orElse(""),
                Optional.ofNullable(question.getAnswer()).orElse("")
        ).toLowerCase(Locale.ROOT);

        LinkedHashSet<Long> ids = new LinkedHashSet<>();

        addIfMatched(ids, knowledgeIdsByName, "集合与常用逻辑",
                containsAny(text, "集合", "a∩b", "a∪b", "命题"));
        addIfMatched(ids, knowledgeIdsByName, "复数",
                containsAny(text, "复数", "z=", "z²", "+i", "-i"));
        addIfMatched(ids, knowledgeIdsByName, "平面向量",
                containsAny(text, "向量", "a·b", "|a+b|", "|a-b|"));
        addIfMatched(ids, knowledgeIdsByName, "概率统计",
                containsAny(text, "概率", "随机变量", "p(", "b("));
        addIfMatched(ids, knowledgeIdsByName, "立体几何",
                containsAny(text, "正方体", "圆柱", "圆锥", "四棱锥", "二面角", "平面"));
        addIfMatched(ids, knowledgeIdsByName, "解析几何",
                containsAny(text, "椭圆", "双曲线", "抛物线", "焦点", "离心率", "渐近线"));
        addIfMatched(ids, knowledgeIdsByName, "三角函数",
                containsAny(text, "sin", "cos", "tan", "三角"));
        addIfMatched(ids, knowledgeIdsByName, "数列",
                containsAny(text, "数列", "an", "sn", "tn", "等比", "等差"));
        addIfMatched(ids, knowledgeIdsByName, "函数与导数",
                containsAny(text, "函数", "导数", "f(", "f'", "ln", "eˣ", "单调", "极值", "零点"));
        addIfMatched(ids, knowledgeIdsByName, "不等式",
                containsAny(text, "不等式", "恒成立", "取值范围", "约束条件", "利润最大", "最大利润"));

        return new ArrayList<>(ids);
    }

    private void addIfMatched(Set<Long> ids, Map<String, Long> knowledgeIdsByName, String name, boolean matched) {
        Long knowledgeId = knowledgeIdsByName.get(name);
        if (matched && knowledgeId != null) {
            ids.add(knowledgeId);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
