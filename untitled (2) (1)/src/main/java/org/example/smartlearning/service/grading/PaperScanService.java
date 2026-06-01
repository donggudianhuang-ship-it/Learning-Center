package org.example.smartlearning.service.grading;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.example.smartlearning.dto.response.ScanGradingResponse;
import org.example.smartlearning.entity.AiGradingRecord;
import org.example.smartlearning.entity.KnowledgeMastery;
import org.example.smartlearning.entity.KnowledgePoint;
import org.example.smartlearning.entity.Subject;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.AiGradingRecordMapper;
import org.example.smartlearning.mapper.KnowledgeMasteryMapper;
import org.example.smartlearning.mapper.KnowledgePointMapper;
import org.example.smartlearning.mapper.SubjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 试卷扫描判卷服务
 * 使用 OCR 提取文字，再用 DeepSeek AI 进行判卷
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperScanService {

    private final ClaudeService claudeService;
    private final AiGradingRecordMapper aiGradingRecordMapper;
    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;

    /**
     * 扫描试卷图片并判卷
     */
    @Transactional
    public ScanGradingResponse scanAndGrade(Long userId, MultipartFile file, Long subjectId, String grade) {
        AiGradingRecord record = userId == null ? null : createProcessingRecord(userId, file, grade);
        try {
            // 1. 使用 OCR 提取图片中的文字
            String ocrText = extractTextFromImage(file);
            log.info("OCR识别结果: {}", ocrText);
            updateProcessingRecordOcr(record, ocrText);

            if (ocrText == null || ocrText.trim().isEmpty()) {
                throw BusinessException.of("无法识别图片内容，请确保图片清晰、文字完整且不要倾斜");
            }

            // 2. 调用 DeepSeek AI 分析试卷内容
            String aiResponse = analyzePaperContent(ocrText, grade);

            // 3. 解析 AI 响应
            ScanGradingResponse result = parseGradingResult(aiResponse);
            if ("FAILED".equals(result.getStatus())) {
                throw BusinessException.of(result.getErrorMessage() == null
                        ? "AI返回结果解析失败，请重新上传或稍后再试"
                        : result.getErrorMessage());
            }
            result.setOcrText(ocrText);
            result.setStatus("SUCCESS");
            syncScanKnowledgeMastery(userId, subjectId, result);

            // 4. 保存 AI 判卷记录，供页面历史记录查看
            finishGradingRecord(record, file, result);
            return result;

        } catch (IOException e) {
            log.error("读取图片失败", e);
            markGradingRecordFailed(record, "读取图片失败：" + friendlyMessage(e));
            throw BusinessException.of("读取图片失败，请重新上传清晰图片");
        } catch (BusinessException e) {
            markGradingRecordFailed(record, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("AI判卷失败", e);
            String message = friendlyMessage(e);
            markGradingRecordFailed(record, message);
            throw BusinessException.of(message);
        }
    }

    /**
     * 使用 Tesseract OCR 提取图片中的文字
     */
    private String extractTextFromImage(MultipartFile file) throws IOException {
        try {
            // 保存临时文件
            Path tempFile = Files.createTempFile("ocr_", getImageExtension(file.getOriginalFilename()));
            file.transferTo(tempFile.toFile());

            // 初始化 Tesseract
            Tesseract tesseract = new Tesseract();

            // 设置 tessdata 路径
            String tessDataPath = System.getProperty("tess.data.path", "./tessdata");
            File tessDataDir = new File(tessDataPath);
            if (!tessDataDir.exists()) {
                // 尝试其他路径
                tessDataPath = "tessdata";
                tessDataDir = new File(tessDataPath);
            }
            tesseract.setDatapath(tessDataPath);

            // 设置语言：中文+英文
            tesseract.setLanguage("chi_sim+eng");

            // 设置 OCR 模式
            tesseract.setOcrEngineMode(1); // LSTM only

            // 读取图片
            BufferedImage image = ImageIO.read(tempFile.toFile());
            if (image == null) {
                throw new RuntimeException("无法读取图片文件");
            }

            // 执行 OCR
            String result = tesseract.doOCR(image);

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            return result;

        } catch (Exception e) {
            log.error("OCR识别失败", e);
            throw BusinessException.of("OCR识别失败，请确认图片清晰、文字无遮挡，或换一张图片重试");
        }
    }

    /**
     * 获取图片扩展名
     */
    private String getImageExtension(String filename) {
        if (filename == null) return ".png";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return ".png";
    }

    /**
     * 调用 DeepSeek AI 分析试卷内容
     */
    private String analyzePaperContent(String ocrText, String grade) {
        String systemPrompt = """
                你是一位严格但讲理的高中教师，负责批改学生上传的试卷/作业。

                OCR识别出的试卷内容如下，请仔细分析并：
                1. 自动识别科目（语文、数学、英语、物理、化学、生物、历史、地理、政治等）
                2. 推断年级（根据题目难度和内容）
                3. 识别每道题目的内容
                4. 识别学生的答案
                5. 给出正确答案
                6. 进行批改评分

                对于客观题（选择题、判断题、填空题）：
                - 直接判断对错
                - 给出分数（满分10分）

                对于主观题（简答题、论述题）：
                - 必须根据题干、标准答案、解析或OCR中的答案依据进行批改
                - 分析答案的完整性和准确性
                - 学生只写一个无关数字/字母/“不会”等，不能判正确
                - 给出合理的分数（满分10分）
                - 提供详细的点评和改进建议
                - isCorrect 只有在 score >= 7 且核心得分点基本完整时才为 true

                只能返回一个 JSON 对象，不要返回 Markdown，不要解释 JSON 之外的内容：
                {
                    "subject": "识别出的科目",
                    "grade": "推断的年级",
                    "questions": [
                        {
                            "questionNumber": 1,
                            "questionContent": "题目内容",
                            "questionType": "SINGLE_CHOICE/TRUE_FALSE/FILL_BLANK/SHORT_ANSWER/ESSAY",
                            "userAnswer": "学生答案",
                            "correctAnswer": "正确答案",
                            "isCorrect": true/false,
                            "score": 分数,
                            "maxScore": 10,
                            "aiAnalysis": "AI分析点评",
                            "mistakeType": "错误类型（如果错误的话）",
                            "knowledgePoints": "涉及的知识点",
                            "suggestion": "改进建议"
                        }
                    ],
                    "summary": "总体评价和学习建议"
                }

                注意：
                - 如果OCR识别有误，请尽量推断正确内容
                - 给出公正、合理的评分
                - 提供有建设性的反馈
                - 必须自动识别科目，不要返回"未知"
                """;

        String userMessage = String.format("""
                用户填写的年级：%s

                以下是OCR识别出的试卷/作业内容，请分析并批改：

                ---
                %s
                ---

                请返回JSON格式的批改结果。
                """, grade == null || grade.isBlank() ? "未填写" : grade, ocrText);

        return claudeService.callClaude(systemPrompt, userMessage);
    }

    /**
     * 解析批改结果
     */
    private ScanGradingResponse parseGradingResult(String aiResponse) {
        ScanGradingResponse response = new ScanGradingResponse();

        try {
            // 尝试提取 JSON 部分
            String jsonStr = aiResponse;
            if (aiResponse.contains("```json")) {
                jsonStr = aiResponse.substring(aiResponse.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else if (aiResponse.contains("```")) {
                jsonStr = aiResponse.substring(aiResponse.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            }

            JSONObject json = JSON.parseObject(jsonStr.trim());

            // 解析科目和年级
            response.setSubject(json.getString("subject"));
            response.setGrade(json.getString("grade"));

            // 解析题目列表
            List<ScanGradingResponse.RecognizedQuestion> questions = new ArrayList<>();
            JSONArray questionsArray = json.getJSONArray("questions");
            if (questionsArray != null) {
                for (int i = 0; i < questionsArray.size(); i++) {
                    JSONObject q = questionsArray.getJSONObject(i);
                    ScanGradingResponse.RecognizedQuestion question = new ScanGradingResponse.RecognizedQuestion();
                    question.setQuestionNumber(q.getInteger("questionNumber"));
                    question.setQuestionContent(q.getString("questionContent"));
                    question.setQuestionType(q.getString("questionType"));
                    question.setUserAnswer(q.getString("userAnswer"));
                    question.setCorrectAnswer(q.getString("correctAnswer"));
                    question.setIsCorrect(q.getBoolean("isCorrect"));
                    question.setScore(q.getBigDecimal("score"));
                    question.setMaxScore(q.getBigDecimal("maxScore"));
                    question.setAiAnalysis(q.getString("aiAnalysis"));
                    question.setMistakeType(q.getString("mistakeType"));
                    question.setKnowledgePoints(q.getString("knowledgePoints"));
                    question.setSuggestion(q.getString("suggestion"));
                    questions.add(question);
                }
            }
            response.setQuestions(questions);

            // 计算总分
            BigDecimal totalScore = questions.stream()
                    .map(ScanGradingResponse.RecognizedQuestion::getScore)
                    .filter(s -> s != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal maxScore = questions.stream()
                    .map(ScanGradingResponse.RecognizedQuestion::getMaxScore)
                    .filter(s -> s != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            response.setTotalScore(totalScore);
            response.setMaxScore(maxScore);

            // 计算正确率
            if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
                response.setAccuracyRate(totalScore.multiply(BigDecimal.valueOf(100))
                        .divide(maxScore, 2, RoundingMode.HALF_UP));
            } else {
                response.setAccuracyRate(BigDecimal.ZERO);
            }

            // 统计正确/错误题数
            int correctCount = (int) questions.stream().filter(q -> Boolean.TRUE.equals(q.getIsCorrect())).count();
            int wrongCount = questions.size() - correctCount;
            response.setCorrectCount(correctCount);
            response.setWrongCount(wrongCount);

            // 设置 AI 总结
            response.setAiSummary(json.getString("summary"));

            // 生成错误点分析
            List<ScanGradingResponse.ErrorPoint> errorPoints = new ArrayList<>();
            for (ScanGradingResponse.RecognizedQuestion q : questions) {
                if (!Boolean.TRUE.equals(q.getIsCorrect())) {
                    ScanGradingResponse.ErrorPoint error = new ScanGradingResponse.ErrorPoint();
                    error.setQuestionNumber(q.getQuestionNumber());
                    error.setQuestionContent(q.getQuestionContent());
                    error.setUserAnswer(q.getUserAnswer());
                    error.setCorrectAnswer(q.getCorrectAnswer());
                    error.setMistakeType(q.getMistakeType());
                    error.setMistakeReason(generateMistakeReason(q));
                    error.setSuggestion(q.getSuggestion());
                    errorPoints.add(error);
                }
            }
            response.setErrorPoints(errorPoints);

        } catch (Exception e) {
            log.warn("解析AI响应失败，使用默认值", e);
            // 如果解析失败，返回原始响应作为总结
            response.setAiSummary(aiResponse);
            response.setQuestions(new ArrayList<>());
            response.setErrorPoints(new ArrayList<>());
            response.setTotalScore(BigDecimal.ZERO);
            response.setMaxScore(BigDecimal.ZERO);
            response.setAccuracyRate(BigDecimal.ZERO);
            response.setCorrectCount(0);
            response.setWrongCount(0);
            response.setStatus("FAILED");
            response.setErrorMessage("AI返回结果解析失败，请重新上传或稍后再试");
        }

        return response;
    }

    public List<Map<String, Object>> getAiGradingRecords(Long userId, Integer limit) {
        int safeLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 20;
        List<AiGradingRecord> records = aiGradingRecordMapper.selectList(
                new LambdaQueryWrapper<AiGradingRecord>()
                        .eq(AiGradingRecord::getUserId, userId)
                        .orderByDesc(AiGradingRecord::getCreatedAt)
                        .last("LIMIT " + safeLimit)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (AiGradingRecord record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("recordId", record.getId());
            item.put("title", record.getTitle());
            item.put("subject", record.getSubject());
            item.put("grade", record.getGrade());
            item.put("totalScore", record.getTotalScore());
            item.put("maxScore", record.getMaxScore());
            item.put("accuracyRate", record.getAccuracyRate());
            item.put("correctCount", record.getCorrectCount());
            item.put("wrongCount", record.getWrongCount());
            item.put("originalFileName", record.getOriginalFileName());
            item.put("status", record.getStatus());
            item.put("errorMessage", record.getErrorMessage());
            item.put("createdAt", record.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    public ScanGradingResponse getAiGradingRecordDetail(Long userId, Long recordId) {
        AiGradingRecord record = aiGradingRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new RuntimeException("AI判卷记录不存在");
        }

        ScanGradingResponse response;
        try {
            response = JSON.parseObject(record.getResultJson(), ScanGradingResponse.class);
        } catch (Exception ex) {
            response = new ScanGradingResponse();
            response.setAiSummary(record.getResultJson());
            response.setQuestions(new ArrayList<>());
            response.setErrorPoints(new ArrayList<>());
        }
        attachRecordMetadata(response, record);
        return response;
    }

    private AiGradingRecord createProcessingRecord(Long userId, MultipartFile file, String grade) {
        AiGradingRecord record = new AiGradingRecord();
        record.setUserId(userId);
        record.setTitle("AI判卷中 - " + defaultFileName(file));
        record.setGrade(grade);
        record.setTotalScore(BigDecimal.ZERO);
        record.setMaxScore(BigDecimal.ZERO);
        record.setAccuracyRate(BigDecimal.ZERO);
        record.setCorrectCount(0);
        record.setWrongCount(0);
        record.setResultJson("{}");
        record.setOriginalFileName(file.getOriginalFilename());
        record.setStatus("PROCESSING");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        aiGradingRecordMapper.insert(record);
        return record;
    }

    private void updateProcessingRecordOcr(AiGradingRecord record, String ocrText) {
        if (record == null) {
            return;
        }
        record.setOcrText(ocrText);
        record.setUpdatedAt(LocalDateTime.now());
        aiGradingRecordMapper.updateById(record);
    }

    private void finishGradingRecord(AiGradingRecord record, MultipartFile file, ScanGradingResponse result) {
        if (record == null) {
            return;
        }
        record.setTitle(buildRecordTitle(result, file));
        record.setSubject(result.getSubject());
        record.setGrade(result.getGrade());
        record.setTotalScore(defaultDecimal(result.getTotalScore()));
        record.setMaxScore(defaultDecimal(result.getMaxScore()));
        record.setAccuracyRate(defaultDecimal(result.getAccuracyRate()));
        record.setCorrectCount(result.getCorrectCount() == null ? 0 : result.getCorrectCount());
        record.setWrongCount(result.getWrongCount() == null ? 0 : result.getWrongCount());
        record.setResultJson(JSON.toJSONString(result));
        record.setOriginalFileName(file.getOriginalFilename());
        record.setOcrText(result.getOcrText());
        record.setStatus("SUCCESS");
        record.setErrorMessage(null);
        record.setUpdatedAt(LocalDateTime.now());
        aiGradingRecordMapper.updateById(record);
        attachRecordMetadata(result, record);
    }

    private void markGradingRecordFailed(AiGradingRecord record, String message) {
        if (record == null) {
            return;
        }
        ScanGradingResponse failed = new ScanGradingResponse();
        failed.setRecordId(record.getId());
        failed.setTitle(record.getTitle());
        failed.setCreatedAt(record.getCreatedAt());
        failed.setSubject(record.getSubject());
        failed.setGrade(record.getGrade());
        failed.setStatus("FAILED");
        failed.setErrorMessage(message);
        failed.setOcrText(record.getOcrText());
        failed.setQuestions(new ArrayList<>());
        failed.setErrorPoints(new ArrayList<>());
        failed.setTotalScore(BigDecimal.ZERO);
        failed.setMaxScore(BigDecimal.ZERO);
        failed.setAccuracyRate(BigDecimal.ZERO);
        failed.setCorrectCount(0);
        failed.setWrongCount(0);
        failed.setAiSummary(message);

        record.setStatus("FAILED");
        record.setErrorMessage(message);
        record.setResultJson(JSON.toJSONString(failed));
        record.setUpdatedAt(LocalDateTime.now());
        aiGradingRecordMapper.updateById(record);
    }

    private void attachRecordMetadata(ScanGradingResponse response, AiGradingRecord record) {
        response.setRecordId(record.getId());
        response.setTitle(record.getTitle());
        response.setCreatedAt(record.getCreatedAt());
        response.setStatus(record.getStatus());
        response.setErrorMessage(record.getErrorMessage());
        response.setOcrText(response.getOcrText() == null ? record.getOcrText() : response.getOcrText());
        response.setSubject(response.getSubject() == null ? record.getSubject() : response.getSubject());
        response.setGrade(response.getGrade() == null ? record.getGrade() : response.getGrade());
        response.setTotalScore(response.getTotalScore() == null ? record.getTotalScore() : response.getTotalScore());
        response.setMaxScore(response.getMaxScore() == null ? record.getMaxScore() : response.getMaxScore());
        response.setAccuracyRate(response.getAccuracyRate() == null ? record.getAccuracyRate() : response.getAccuracyRate());
        response.setCorrectCount(response.getCorrectCount() == null ? record.getCorrectCount() : response.getCorrectCount());
        response.setWrongCount(response.getWrongCount() == null ? record.getWrongCount() : response.getWrongCount());
    }

    private String buildRecordTitle(ScanGradingResponse result, MultipartFile file) {
        String subject = result.getSubject() == null || result.getSubject().isBlank() ? "AI" : result.getSubject();
        String filename = defaultFileName(file);
        if (filename.isBlank()) {
            return subject + "判卷记录";
        }
        return subject + "判卷 - " + filename;
    }

    private String defaultFileName(MultipartFile file) {
        String filename = file == null ? null : file.getOriginalFilename();
        return filename == null || filename.isBlank() ? "上传图片" : filename;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void syncScanKnowledgeMastery(Long userId, Long explicitSubjectId, ScanGradingResponse result) {
        if (userId == null || result == null || result.getQuestions() == null || result.getQuestions().isEmpty()) {
            return;
        }
        Long subjectId = resolveSubjectId(explicitSubjectId, result.getSubject());
        if (subjectId == null) {
            return;
        }
        List<KnowledgePoint> subjectPoints = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getSubjectId, subjectId)
        );
        if (subjectPoints.isEmpty()) {
            return;
        }

        for (ScanGradingResponse.RecognizedQuestion question : result.getQuestions()) {
            Set<Long> knowledgeIds = resolveScanKnowledgeIds(question.getKnowledgePoints(), subjectPoints);
            for (Long knowledgeId : knowledgeIds) {
                updateMasteryFromScan(userId, knowledgeId, Boolean.TRUE.equals(question.getIsCorrect()));
            }
        }
    }

    private Long resolveSubjectId(Long explicitSubjectId, String subjectName) {
        if (explicitSubjectId != null) {
            return explicitSubjectId;
        }
        if (subjectName == null || subjectName.isBlank()) {
            return null;
        }
        Subject subject = subjectMapper.selectOne(
                new LambdaQueryWrapper<Subject>()
                        .eq(Subject::getName, subjectName.trim())
                        .last("LIMIT 1")
        );
        if (subject != null) {
            return subject.getId();
        }
        List<Subject> subjects = subjectMapper.selectList(new LambdaQueryWrapper<>());
        return subjects.stream()
                .filter(item -> item.getName() != null
                        && (subjectName.contains(item.getName()) || item.getName().contains(subjectName)))
                .map(Subject::getId)
                .findFirst()
                .orElse(null);
    }

    private Set<Long> resolveScanKnowledgeIds(String knowledgePoints, List<KnowledgePoint> subjectPoints) {
        if (knowledgePoints == null || knowledgePoints.isBlank()) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        String[] tokens = knowledgePoints.split("[,，、;；/\\n]");
        for (String token : tokens) {
            String normalized = token.trim();
            if (normalized.isBlank()) {
                continue;
            }
            subjectPoints.stream()
                    .filter(point -> point.getName() != null)
                    .filter(point -> Objects.equals(point.getName(), normalized)
                            || normalized.contains(point.getName())
                            || point.getName().contains(normalized))
                    .map(KnowledgePoint::getId)
                    .findFirst()
                    .ifPresent(ids::add);
        }
        return ids;
    }

    private void updateMasteryFromScan(Long userId, Long knowledgeId, boolean isCorrect) {
        KnowledgeMastery mastery = knowledgeMasteryMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getKnowledgeId, knowledgeId)
                        .last("LIMIT 1")
        );
        if (mastery == null) {
            mastery = new KnowledgeMastery();
            mastery.setUserId(userId);
            mastery.setKnowledgeId(knowledgeId);
            mastery.setTotalQuestions(0);
            mastery.setCorrectQuestions(0);
            mastery.setCreatedAt(LocalDateTime.now());
        }

        int total = mastery.getTotalQuestions() == null ? 0 : mastery.getTotalQuestions();
        int correct = mastery.getCorrectQuestions() == null ? 0 : mastery.getCorrectQuestions();
        total++;
        if (isCorrect) {
            correct++;
        }
        mastery.setTotalQuestions(total);
        mastery.setCorrectQuestions(correct);
        mastery.setMasteryLevel(BigDecimal.valueOf(correct)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        mastery.setLastPracticeAt(LocalDateTime.now());
        mastery.setUpdatedAt(LocalDateTime.now());

        if (mastery.getId() == null) {
            knowledgeMasteryMapper.insert(mastery);
        } else {
            knowledgeMasteryMapper.updateById(mastery);
        }
    }

    private String friendlyMessage(Exception e) {
        String raw = e.getMessage() == null ? "" : e.getMessage();
        if (raw.contains("API Key") || raw.contains("DEEPSEEK_API_KEY") || raw.contains("claude.api-key")) {
            return "AI服务未配置API Key，请先配置 DEEPSEEK_API_KEY 后再判卷";
        }
        if (raw.contains("timeout") || raw.contains("timed out")) {
            return "AI服务响应超时，请稍后重试";
        }
        if (raw.contains("OCR")) {
            return "OCR识别失败，请确认图片清晰、文字无遮挡，或换一张图片重试";
        }
        if (raw.isBlank()) {
            return "AI判卷失败，请稍后重试";
        }
        return raw.length() > 120 ? raw.substring(0, 120) : raw;
    }

    /**
     * 生成错误原因
     */
    private String generateMistakeReason(ScanGradingResponse.RecognizedQuestion question) {
        String mistakeType = question.getMistakeType();
        if (mistakeType == null || mistakeType.isEmpty()) {
            return "答案有误，请参考正确答案进行复习";
        }

        return switch (mistakeType) {
            case "CONCEPT_ERROR" -> "概念理解错误，需要重新学习相关知识点";
            case "CARELESS" -> "粗心大意，需要更加仔细审题";
            case "WRONG_APPROACH" -> "解题思路错误，需要掌握正确的解题方法";
            case "INCOMPLETE" -> "答案不完整，遗漏了关键要点";
            case "KNOWLEDGE_GAP" -> "知识点掌握不牢固，需要加强练习";
            case "CALCULATION_ERROR" -> "计算错误，需要检查计算过程";
            default -> "答案有误，请参考正确答案进行复习";
        };
    }
}
