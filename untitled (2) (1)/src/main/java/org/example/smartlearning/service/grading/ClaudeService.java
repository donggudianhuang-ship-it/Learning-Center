package org.example.smartlearning.service.grading;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.smartlearning.config.ClaudeConfig;
import org.example.smartlearning.dto.response.GradingResultResponse;
import org.example.smartlearning.entity.Question;
import org.example.smartlearning.entity.Subject;
import org.example.smartlearning.mapper.SubjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * AI服务类
 * 用于智能判卷、语义分析、个性化推荐。
 * 当前按 OpenAI-compatible 接口调用，适配 DeepSeek 等模型服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    private final ClaudeConfig.ClaudeProperties claudeProperties;
    private final SubjectMapper subjectMapper;

    /**
     * 调用 OpenAI-compatible Chat Completions API
     */
    public String callClaude(String systemPrompt, String userMessage) {
        if (claudeProperties.apiKey() == null || claudeProperties.apiKey().isBlank()) {
            throw new RuntimeException("AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY");
        }

        RestClient restClient = RestClient.create();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", claudeProperties.model());
        requestBody.put("max_tokens", claudeProperties.maxTokens());

        Map<String, Object> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userMessage);
        requestBody.put("messages", new Map[]{system, message});

        try {
            String response = restClient.post()
                    .uri(claudeProperties.apiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + claudeProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.toJSONString(requestBody))
                    .retrieve()
                    .body(String.class);

            JSONObject jsonResponse = JSON.parseObject(response);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject messageObj = choices.getJSONObject(0).getJSONObject("message");
                if (messageObj != null) {
                    return messageObj.getString("content");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("调用AI API失败", e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 批改客观题（选择题、判断题）
     */
    public GradingResultResponse gradeObjectiveQuestion(Question question, String userAnswer) {
        GradingResultResponse result = new GradingResultResponse();
        result.setQuestionId(question.getId());
        result.setMaxScore(BigDecimal.TEN);

        String correctAnswer = normalizeObjectiveAnswer(question.getAnswer());
        String userAnswerTrimmed = normalizeObjectiveAnswer(userAnswer);

        boolean isCorrect = correctAnswer.equals(userAnswerTrimmed);
        result.setIsCorrect(isCorrect);
        result.setScore(isCorrect ? BigDecimal.TEN : BigDecimal.ZERO);
        result.setCorrectAnswer(question.getAnswer());
        result.setAnalysis(question.getAnalysis());

        if (!isCorrect) {
            result.setMistakeType("KNOWLEDGE_GAP");
            result.setAiAnalysis("答案错误。正确答案是: " + question.getAnswer() + "。" + question.getAnalysis());
        } else {
            result.setAiAnalysis("回答正确！" + question.getAnalysis());
        }

        return result;
    }

    private String normalizeObjectiveAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        String normalized = answer.trim().toUpperCase()
                .replaceAll("[\\s,，、;；]+", "");
        if (normalized.matches("[A-D]+")) {
            char[] chars = normalized.toCharArray();
            java.util.Arrays.sort(chars);
            return new String(chars);
        }
        return normalized;
    }

    /**
     * 批改主观题（简答题、论述题）
     */
    public GradingResultResponse gradeSubjectiveQuestion(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return strictSubjectiveFallback(question, userAnswer, "学生未作答，主观题不能判为正确。");
        }

        String subjectName = getSubjectName(question.getSubjectId());
        String typeName = getQuestionTypeName(question.getType(), subjectName);
        String systemPrompt = """
                你是一位严格但讲理的高中教师，负责批改学生主观题。
                必须只依据题干、标准答案、答案解析和学生答案判分，不要自行扩展标准。

                判分要求：
                1. 数学计算题：重点检查最终结论、关键步骤、方法方向。只有最终答案等价但过程缺失时可给部分分；最终答案错误不能判正确。
                2. 生物非选择题：按标准答案和解析中的得分点、关键词、因果关系、实验结论逐项判断。
                3. 学生答案为空、明显过短、只写一个无关数字/字母/“不会”等，不能判正确。
                4. score 为 0-10 分，isCorrect 只有在 score >= 7 且核心得分点基本完整时才为 true。
                5. mistakeType 只能是 CORRECT、INCOMPLETE、WRONG_APPROACH、CONCEPT_ERROR、KNOWLEDGE_GAP、CARELESS、NO_ANSWER。

                只能返回一个 JSON 对象，不要返回 Markdown，不要解释 JSON 之外的内容。
                JSON字段：
                {
                  "score": 0,
                  "isCorrect": false,
                  "mistakeType": "INCOMPLETE",
                  "analysis": "批改理由和改进建议",
                  "hitPoints": ["命中的得分点"],
                  "missingPoints": ["缺失的得分点"]
                }
                """;

        String userMessage = String.format("""
                学科：%s
                题型：%s
                题目：%s
                参考答案：%s
                答案解析：%s
                学生答案：%s

                请根据参考答案和答案解析批改学生答案。
                """, subjectName, typeName, question.getContent(), question.getAnswer(), question.getAnalysis(), userAnswer);

        String aiResponse = callClaude(systemPrompt, userMessage);

        GradingResultResponse result = new GradingResultResponse();
        result.setQuestionId(question.getId());
        result.setMaxScore(BigDecimal.TEN);
        result.setCorrectAnswer(question.getAnswer());
        result.setAnalysis(question.getAnalysis());

        try {
            JSONObject json = JSON.parseObject(extractJson(aiResponse));
            BigDecimal score = clampScore(json.getBigDecimal("score"));
            boolean isCorrect = score.compareTo(BigDecimal.valueOf(7)) >= 0
                    && Boolean.TRUE.equals(json.getBoolean("isCorrect"));
            result.setScore(score);
            result.setIsCorrect(isCorrect);
            result.setMistakeType(normalizeMistakeType(json.getString("mistakeType"), isCorrect));
            result.setAiAnalysis(buildAiAnalysis(json));

            if (isUnsafeShortAnswer(question, userAnswer)) {
                result.setScore(result.getScore().min(BigDecimal.valueOf(2)));
                result.setIsCorrect(false);
                result.setMistakeType("INCOMPLETE");
                result.setAiAnalysis("学生答案过短或信息不足，不能判为正确。"
                        + (result.getAiAnalysis() == null ? "" : "\n" + result.getAiAnalysis()));
            }
        } catch (Exception e) {
            log.warn("解析AI响应失败，使用严格兜底判分", e);
            return strictSubjectiveFallback(question, userAnswer,
                    "AI返回格式异常，系统已使用严格兜底判分。AI原始反馈：" + aiResponse);
        }

        return result;
    }

    public GradingResultResponse strictSubjectiveFallback(Question question, String userAnswer, String reason) {
        String answer = userAnswer == null ? "" : userAnswer.trim();
        String reference = question.getAnswer() == null ? "" : question.getAnswer().trim();
        String normalizedAnswer = normalizeText(answer);
        String normalizedReference = normalizeText(reference);

        boolean exact = !normalizedAnswer.isEmpty() && normalizedAnswer.equals(normalizedReference);
        boolean numericExact = isNumericExact(answer, reference);

        GradingResultResponse result = new GradingResultResponse();
        result.setQuestionId(question.getId());
        result.setMaxScore(BigDecimal.TEN);
        result.setCorrectAnswer(question.getAnswer());
        result.setAnalysis(question.getAnalysis());
        result.setIsCorrect(exact || numericExact);
        result.setScore(result.getIsCorrect() ? BigDecimal.TEN : BigDecimal.ZERO);
        result.setMistakeType(result.getIsCorrect() ? "CORRECT" : answer.isBlank() ? "NO_ANSWER" : "INCOMPLETE");
        result.setAiAnalysis(reason);
        return result;
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = score.setScale(1, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (normalized.compareTo(BigDecimal.TEN) > 0) {
            return BigDecimal.TEN;
        }
        return normalized;
    }

    private String buildAiAnalysis(JSONObject json) {
        String analysis = json.getString("analysis");
        String hitPoints = json.getString("hitPoints");
        String missingPoints = json.getString("missingPoints");
        StringBuilder builder = new StringBuilder(analysis == null ? "" : analysis.trim());
        if (hitPoints != null && !hitPoints.isBlank()) {
            builder.append(builder.isEmpty() ? "" : "\n").append("命中得分点：").append(hitPoints);
        }
        if (missingPoints != null && !missingPoints.isBlank()) {
            builder.append(builder.isEmpty() ? "" : "\n").append("缺失得分点：").append(missingPoints);
        }
        return builder.toString();
    }

    private String normalizeMistakeType(String mistakeType, boolean isCorrect) {
        if (isCorrect) {
            return "CORRECT";
        }
        if (mistakeType == null || mistakeType.isBlank()) {
            return "KNOWLEDGE_GAP";
        }
        return switch (mistakeType) {
            case "INCOMPLETE", "WRONG_APPROACH", "CONCEPT_ERROR", "KNOWLEDGE_GAP", "CARELESS", "NO_ANSWER" -> mistakeType;
            default -> "KNOWLEDGE_GAP";
        };
    }

    private boolean isUnsafeShortAnswer(Question question, String userAnswer) {
        String normalized = normalizeText(userAnswer);
        if (normalized.isEmpty()) {
            return true;
        }
        String reference = question.getAnswer() == null ? "" : question.getAnswer();
        if (normalized.length() <= 2) {
            return !normalizeText(reference).equals(normalized) && !isNumericExact(userAnswer, reference);
        }
        String subjectName = getSubjectName(question.getSubjectId());
        if ("生物".equals(subjectName) && "ESSAY".equals(question.getType()) && normalized.length() < 6) {
            return true;
        }
        return normalized.matches("不会|不知道|不懂|无|没有|略|乱写");
    }

    private boolean isNumericExact(String answer, String reference) {
        String answerNumber = extractSingleNumber(answer);
        if (answerNumber == null) {
            return false;
        }
        String referenceNumber = extractSingleNumber(reference);
        if (referenceNumber == null) {
            return false;
        }
        try {
            BigDecimal a = new BigDecimal(answerNumber);
            BigDecimal r = new BigDecimal(referenceNumber);
            return a.compareTo(r) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String extractSingleNumber(String text) {
        String normalized = normalizeText(text);
        if (normalized.matches("-?\\d+(\\.\\d+)?")) {
            return normalized;
        }
        return null;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", "")
                .replaceAll("[，。；：、,.!:;()（）【】\\[\\]<>《》=＝]", "")
                .toUpperCase();
    }

    private String getSubjectName(Long subjectId) {
        if (subjectId == null) {
            return "未分类";
        }
        Subject subject = subjectMapper.selectById(subjectId);
        return subject == null ? "未分类" : subject.getName();
    }

    private String getQuestionTypeName(String type, String subjectName) {
        if ("数学".equals(subjectName) && "ESSAY".equals(type)) {
            return "计算题";
        }
        if ("生物".equals(subjectName) && "ESSAY".equals(type)) {
            return "非选择题";
        }
        return switch (type) {
            case "SHORT_ANSWER" -> "简答题";
            case "ESSAY" -> "主观题";
            case "FILL_BLANK" -> "填空题";
            default -> type == null ? "未知题型" : type;
        };
    }

    private String extractJson(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return "{}";
        }
        String jsonStr = aiResponse.trim();
        if (jsonStr.contains("```json")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
        } else if (jsonStr.contains("```")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
        } else if (jsonStr.contains("{") && jsonStr.contains("}")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1);
        }
        return jsonStr.trim();
    }

    /**
     * 分析错题原因
     */
    public String analyzeMistakeReason(Question question, String userAnswer) {
        String systemPrompt = """
                你是一位专业的学习分析师，请分析学生答错题目的原因。
                错误类型包括：
                - CONCEPT_ERROR: 概念理解错误
                - CARELESS: 粗心大意
                - WRONG_APPROACH: 解题思路错误
                - INCOMPLETE: 答案不完整
                - KNOWLEDGE_GAP: 知识点缺失

                请返回JSON格式：{"mistakeType": "类型", "reason": "详细原因", "suggestion": "改进建议"}
                """;

        String userMessage = String.format("""
                题目：%s
                正确答案：%s
                学生答案：%s
                题目解析：%s

                请分析学生答错的原因。
                """, question.getContent(), question.getAnswer(), userAnswer, question.getAnalysis());

        return callClaude(systemPrompt, userMessage);
    }

    /**
     * 生成学习报告
     */
    public String generateLearningReport(String summaryData) {
        String systemPrompt = """
                你是一位专业的学习顾问，请根据学生的学习数据生成一份详细的学习报告。
                报告应包括：
                1. 整体学习情况概述
                2. 优势与薄弱环节分析
                3. 具体的改进建议
                4. 下阶段学习计划建议

                请用鼓励性的语言，给出具体可行的建议。
                """;

        return callClaude(systemPrompt, summaryData);
    }

    /**
     * 智能推荐题目
     */
    public String recommendQuestions(String userProfile, String weakPoints) {
        String systemPrompt = """
                你是一位智能学习助手，请根据学生的薄弱环节推荐合适的练习题目类型。
                推荐应考虑：
                1. 知识点的关联性
                2. 难度的递进性
                3. 学习效率最大化

                请返回JSON数组格式的推荐列表。
                """;

        String userMessage = String.format("""
                学生学习档案：%s
                薄弱知识点：%s

                请推荐适合的练习方向。
                """, userProfile, weakPoints);

        return callClaude(systemPrompt, userMessage);
    }

    /**
     * 深度分析主观题答案
     * 返回详细的错误点定位
     */
    public String analyzeSubjectiveAnswer(Question question, String userAnswer) {
        String systemPrompt = """
                你是一位资深教师，请对学生答案进行深度分析。
                需要逐点分析：
                1. 答案中的正确部分
                2. 答案中的错误或遗漏部分
                3. 每个错误点的具体原因
                4. 针对性的改进建议

                请以JSON格式返回：
                {
                    "correctPoints": ["正确的要点1", "正确的要点2"],
                    "wrongPoints": [
                        {
                            "content": "错误内容",
                            "reason": "错误原因",
                            "correction": "正确内容"
                        }
                    ],
                    "missingPoints": ["遗漏的要点1", "遗漏的要点2"],
                    "overallAnalysis": "总体评价",
                    "suggestions": ["建议1", "建议2"]
                }
                """;

        String userMessage = String.format("""
                题目：%s
                参考答案：%s
                学生答案：%s

                请逐点分析学生答案。
                """, question.getContent(), question.getAnswer(), userAnswer);

        return callClaude(systemPrompt, userMessage);
    }

    /**
     * 批量分析错题
     */
    public String batchAnalyzeMistakes(String questionsAndAnswers) {
        String systemPrompt = """
                你是一位学习诊断专家，请分析学生的错题模式。
                需要识别：
                1. 主要错误类型分布
                2. 薄弱知识点
                3. 学习习惯问题
                4. 改进优先级

                请以JSON格式返回分析结果。
                """;

        return callClaude(systemPrompt, questionsAndAnswers);
    }
}
