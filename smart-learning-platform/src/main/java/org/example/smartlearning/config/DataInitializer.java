package org.example.smartlearning.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.smartlearning.entity.*;
import org.example.smartlearning.mapper.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 数据初始化器
 * 应用启动时自动插入测试数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SubjectMapper subjectMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ExamPaperMapper examPaperMapper;
    private final ExamQuestionMapper examQuestionMapper;

    @Override
    public void run(String... args) {
        log.info("开始初始化测试数据...");

        initSubjects();
        initKnowledgePoints();
        initCourses();
        initTestUser();
        initExamPapers();

        log.info("测试数据初始化完成！");
    }

    private void initSubjects() {
        List<Subject> subjects = Arrays.asList(
                createSubject("语文", "语文学科", "📖", 1),
                createSubject("数学", "数学学科", "📐", 2),
                createSubject("英语", "英语学科", "🔤", 3),
                createSubject("物理", "物理学科", "⚡", 4),
                createSubject("化学", "化学学科", "🧪", 5),
                createSubject("生物", "生物学科", "🧬", 6),
                createSubject("历史", "历史学科", "🏛", 7),
                createSubject("地理", "地理学科", "🌏", 8),
                createSubject("政治", "政治学科", "⚖", 9)
        );

        int inserted = 0;
        for (Subject subject : subjects) {
            if (findSubjectByName(subject.getName()) == null) {
                subjectMapper.insert(subject);
                inserted++;
            }
        }
        log.info("补齐 {} 个科目", inserted);
    }

    private Subject createSubject(String name, String description, String icon, int sortOrder) {
        Subject subject = new Subject();
        subject.setName(name);
        subject.setDescription(description);
        subject.setIcon(icon);
        subject.setSortOrder(sortOrder);
        subject.setCreatedAt(LocalDateTime.now());
        return subject;
    }

    private Subject findSubjectByName(String name) {
        return subjectMapper.selectOne(
                new LambdaQueryWrapper<Subject>()
                        .eq(Subject::getName, name)
                        .orderByAsc(Subject::getId)
                        .last("LIMIT 1")
        );
    }

    private Long getSubjectId(String name) {
        Subject subject = findSubjectByName(name);
        return subject == null ? null : subject.getId();
    }

    private void initKnowledgePoints() {
        Subject mathSubject = findSubjectByName("数学");
        if (mathSubject == null) {
            log.info("未找到数学科目，跳过知识点初始化");
            return;
        }

        Long mathSubjectId = mathSubject.getId();
        int inserted = 0;
        inserted += ensureKnowledgePoint(mathSubjectId, "函数与导数", "函数的概念、性质及导数的应用");
        inserted += ensureKnowledgePoint(mathSubjectId, "三角函数", "正弦、余弦、正切函数及其性质");
        inserted += ensureKnowledgePoint(mathSubjectId, "数列", "等差数列、等比数列及求和方法");
        inserted += ensureKnowledgePoint(mathSubjectId, "立体几何", "空间几何体的结构与计算");
        inserted += ensureKnowledgePoint(mathSubjectId, "解析几何", "直线、圆、椭圆、双曲线、抛物线");
        inserted += ensureKnowledgePoint(mathSubjectId, "概率统计", "随机变量、概率模型与统计分析");
        inserted += ensureKnowledgePoint(mathSubjectId, "平面向量", "向量运算、数量积与几何应用");
        inserted += ensureKnowledgePoint(mathSubjectId, "不等式", "不等式求解、恒成立与线性规划");
        inserted += ensureKnowledgePoint(mathSubjectId, "集合与常用逻辑", "集合运算、命题与常用逻辑");
        inserted += ensureKnowledgePoint(mathSubjectId, "复数", "复数运算与几何意义");

        log.info("补齐 {} 个数学知识点", inserted);
    }

    private int ensureKnowledgePoint(Long subjectId, String name, String description) {
        Long count = knowledgePointMapper.selectCount(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getSubjectId, subjectId)
                        .eq(KnowledgePoint::getName, name)
        );
        if (count > 0) {
            return 0;
        }
        knowledgePointMapper.insert(createKnowledgePoint(subjectId, name, 0L, 1, description));
        return 1;
    }

    private KnowledgePoint createKnowledgePoint(Long subjectId, String name, Long parentId, Integer level, String description) {
        KnowledgePoint point = new KnowledgePoint();
        point.setSubjectId(subjectId);
        point.setName(name);
        point.setParentId(parentId);
        point.setLevel(level);
        point.setDescription(description);
        point.setCreatedAt(LocalDateTime.now());
        return point;
    }

    private void initCourses() {
        if (courseMapper.selectCount(null) > 0) {
            log.info("课程数据已存在，跳过初始化");
            return;
        }

        Long mathSubjectId = getSubjectId("数学");
        Long chineseSubjectId = getSubjectId("语文");
        Long englishSubjectId = getSubjectId("英语");
        if (mathSubjectId == null || chineseSubjectId == null || englishSubjectId == null) {
            log.info("核心科目未补齐，跳过课程初始化");
            return;
        }

        List<Course> courses = Arrays.asList(
                createCourse("高考数学函数专题突破", "系统讲解函数的概念、性质、图像及导数的应用，帮助考生突破函数难点", mathSubjectId, 45, "张老师", 1256),
                createCourse("三角函数解题技巧", "掌握三角函数的核心公式和解题方法，提高解题速度和准确率", mathSubjectId, 30, "李老师", 892),
                createCourse("数列求和方法总结", "等差、等比数列及复杂数列的求和方法全解析", mathSubjectId, 35, "王老师", 756),
                createCourse("高考语文古诗词鉴赏", "从意象、手法、情感三个维度深入分析古诗词", chineseSubjectId, 40, "陈老师", 1103),
                createCourse("文言文翻译技巧", "掌握文言文翻译的基本原则和常用技巧", chineseSubjectId, 25, "刘老师", 678),
                createCourse("高考英语语法精讲", "系统梳理高考英语语法考点，突破语法难点", englishSubjectId, 50, "赵老师", 1532),
                createCourse("英语阅读理解技巧", "快速定位、推理判断、主旨大意等题型解题技巧", englishSubjectId, 35, "孙老师", 987),
                createCourse("高考英语写作高分技巧", "作文模板、高级词汇、句型升级全攻略", englishSubjectId, 28, "周老师", 845)
        );

        courses.forEach(courseMapper::insert);
        log.info("初始化 {} 门课程", courses.size());
    }

    private Course createCourse(String title, String description, Long subjectId, Integer duration, String teacherName, Integer viewCount) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setSubjectId(subjectId);
        course.setDuration(duration);
        course.setTeacherName(teacherName);
        course.setViewCount(viewCount);
        course.setLikeCount(viewCount / 10);
        course.setDifficulty(3);
        course.setStatus(1);
        course.setCreatedAt(LocalDateTime.now());
        return course;
    }

    private void initTestUser() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "test");
        if (userMapper.selectCount(wrapper) > 0) {
            log.info("测试用户已存在，跳过初始化");
            return;
        }

        User user = new User();
        user.setUsername("test");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());

        userMapper.insert(user);
        log.info("初始化测试用户: test / 123456");
    }

    private void initExamPapers() {
        Subject mathSubject = subjectMapper.selectOne(
                new LambdaQueryWrapper<Subject>().eq(Subject::getName, "数学").last("LIMIT 1")
        );
        if (mathSubject == null) {
            log.info("未找到数学科目，跳过试卷初始化");
            return;
        }

        List<Question> mathQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSubjectId, mathSubject.getId())
                        .likeRight(Question::getSource, "2025")
        );
        if (mathQuestions.isEmpty()) {
            log.info("未找到2025新高考数学题，跳过试卷初始化");
            return;
        }
        mathQuestions.sort(
                Comparator.comparingInt((Question q) -> extractQuestionNumber(q.getContent()))
                        .thenComparing(Question::getId)
        );

        ExamPaper mathExam = examPaperMapper.selectOne(
                new LambdaQueryWrapper<ExamPaper>()
                        .eq(ExamPaper::getName, "2025新高考一卷数学演练")
                        .last("LIMIT 1")
        );
        if (mathExam == null) {
            mathExam = new ExamPaper();
            mathExam.setName("2025新高考一卷数学演练");
            mathExam.setCreatedAt(LocalDateTime.now());
            examPaperMapper.insert(fillMathExam(mathExam, mathSubject.getId()));
        } else {
            examPaperMapper.updateById(fillMathExam(mathExam, mathSubject.getId()));
        }

        examQuestionMapper.delete(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, mathExam.getId())
        );

        for (Question q : mathQuestions) {
            int questionNumber = extractQuestionNumber(q.getContent());
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(mathExam.getId());
            eq.setQuestionId(q.getId());
            eq.setScore(scoreForQuestion(questionNumber));
            eq.setSortOrder(questionNumber);
            examQuestionMapper.insert(eq);
        }

        log.info("补齐试卷: {}，题目 {} 道", mathExam.getName(), mathQuestions.size());
    }

    private ExamPaper fillMathExam(ExamPaper mathExam, Long subjectId) {
        mathExam.setName("2025新高考一卷数学演练");
        mathExam.setSubjectId(subjectId);
        mathExam.setCreatorId(findCreatorId());
        mathExam.setTotalScore(new BigDecimal("150"));
        mathExam.setDuration(120);
        mathExam.setDescription("保留的2025新高考一卷数学题，按知识点完成标注后用于诊断练习");
        mathExam.setStatus(1);
        mathExam.setUpdatedAt(LocalDateTime.now());
        return mathExam;
    }

    private Long findCreatorId() {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "ADMIN")
                        .orderByAsc(User::getId)
                        .last("LIMIT 1")
        );
        if (admin != null) {
            return admin.getId();
        }

        User firstUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .orderByAsc(User::getId)
                        .last("LIMIT 1")
        );
        return firstUser == null ? 1L : firstUser.getId();
    }

    private BigDecimal scoreForQuestion(int questionNumber) {
        if (questionNumber >= 1 && questionNumber <= 8) {
            return new BigDecimal("5");
        }
        if (questionNumber >= 9 && questionNumber <= 12) {
            return new BigDecimal("6");
        }
        if (questionNumber >= 13 && questionNumber <= 16) {
            return new BigDecimal("5");
        }
        if (questionNumber == 17) {
            return new BigDecimal("10");
        }
        return new BigDecimal("12");
    }

    private int extractQuestionNumber(String content) {
        if (content == null) {
            return 999;
        }
        int start = content.indexOf("第");
        int end = content.indexOf("题", start + 1);
        if (start < 0 || end <= start) {
            return 999;
        }
        try {
            return Integer.parseInt(content.substring(start + 1, end));
        } catch (NumberFormatException ignored) {
            return 999;
        }
    }
}
