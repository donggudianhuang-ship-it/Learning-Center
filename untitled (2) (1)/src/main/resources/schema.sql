-- Smart Learning Platform Database Schema
-- Create database
CREATE DATABASE IF NOT EXISTS smart_learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_learning;

-- User table
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') DEFAULT 'STUDENT',
    status TINYINT DEFAULT 1 COMMENT '0-disabled, 1-active',
    grade VARCHAR(20) COMMENT '高一/高二/高三',
    age INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Subject table
CREATE TABLE IF NOT EXISTS subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(255),
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_subject_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Knowledge point table
CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    UNIQUE KEY uk_subject_knowledge_name (subject_id, name),
    INDEX idx_subject (subject_id),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Question table
CREATE TABLE IF NOT EXISTS question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    knowledge_ids VARCHAR(500) COMMENT 'JSON array of knowledge point IDs',
    type ENUM('SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'SHORT_ANSWER', 'ESSAY') NOT NULL,
    content TEXT NOT NULL,
    options TEXT COMMENT 'JSON array for choices',
    answer TEXT NOT NULL,
    analysis TEXT,
    difficulty TINYINT DEFAULT 3 COMMENT '1-5, 1=easy, 5=hard',
    source VARCHAR(255),
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    INDEX idx_subject (subject_id),
    INDEX idx_type (type),
    INDEX idx_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Exam paper table
CREATE TABLE IF NOT EXISTS exam_paper (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    subject_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    total_score DECIMAL(5,1) DEFAULT 100,
    duration INT DEFAULT 120 COMMENT 'minutes',
    description TEXT,
    status TINYINT DEFAULT 1 COMMENT '0-draft, 1-published',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    FOREIGN KEY (creator_id) REFERENCES user(id),
    INDEX idx_subject (subject_id),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Exam question relation table
CREATE TABLE IF NOT EXISTS exam_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score DECIMAL(5,1) DEFAULT 10,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (exam_id) REFERENCES exam_paper(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id),
    UNIQUE KEY uk_exam_question (exam_id, question_id),
    INDEX idx_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Answer record table
CREATE TABLE IF NOT EXISTS answer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    exam_submission_id BIGINT COMMENT 'NULL if practice mode',
    user_answer TEXT,
    is_correct TINYINT COMMENT '0-wrong, 1-correct, NULL for subjective',
    score DECIMAL(5,1) DEFAULT 0,
    ai_analysis TEXT COMMENT 'AI analysis of the answer',
    mistake_type VARCHAR(50) COMMENT 'CONCEPT_ERROR, CARELESS, WRONG_APPROACH, etc.',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (question_id) REFERENCES question(id),
    INDEX idx_user (user_id),
    INDEX idx_question (question_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Exam submission table
CREATE TABLE IF NOT EXISTS exam_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    total_score DECIMAL(5,1) DEFAULT 0,
    ai_report TEXT COMMENT 'AI generated report',
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status TINYINT DEFAULT 1 COMMENT '0-in progress, 1-submitted, 2-graded',
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (exam_id) REFERENCES exam_paper(id),
    INDEX idx_user (user_id),
    INDEX idx_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI scan grading record table
CREATE TABLE IF NOT EXISTS ai_grading_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    subject VARCHAR(50),
    grade VARCHAR(50),
    total_score DECIMAL(7,2) DEFAULT 0,
    max_score DECIMAL(7,2) DEFAULT 0,
    accuracy_rate DECIMAL(7,2) DEFAULT 0,
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    result_json LONGTEXT NOT NULL,
    original_file_name VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_ai_grading_record_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mistake book table
CREATE TABLE IF NOT EXISTS mistake_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    mistake_type VARCHAR(50),
    review_count INT DEFAULT 0,
    last_review_at DATETIME,
    next_review_date DATE,
    mastery_level TINYINT DEFAULT 0 COMMENT '0-5, 0=not mastered, 5=fully mastered',
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (question_id) REFERENCES question(id),
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_user (user_id),
    INDEX idx_next_review (next_review_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Course table
CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    subject_id BIGINT,
    description TEXT,
    cover_image VARCHAR(255),
    video_url VARCHAR(500),
    duration INT COMMENT 'seconds',
    teacher_id BIGINT,
    teacher_name VARCHAR(100),
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    difficulty TINYINT DEFAULT 3,
    status TINYINT DEFAULT 1 COMMENT '0-draft, 1-published',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    FOREIGN KEY (teacher_id) REFERENCES user(id),
    INDEX idx_subject (subject_id),
    INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Course progress table
CREATE TABLE IF NOT EXISTS course_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    progress INT DEFAULT 0 COMMENT 'percentage 0-100',
    last_position INT DEFAULT 0 COMMENT 'seconds',
    completed TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    UNIQUE KEY uk_user_course (user_id, course_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Study note table
CREATE TABLE IF NOT EXISTS study_note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT,
    subject_id BIGINT,
    knowledge_id BIGINT,
    is_public TINYINT DEFAULT 0,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    collect_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    INDEX idx_user (user_id),
    INDEX idx_subject (subject_id),
    INDEX idx_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Community post table
CREATE TABLE IF NOT EXISTS community_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT,
    subject_id BIGINT,
    anonymous TINYINT DEFAULT 0,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '0-hidden, 1-visible',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    INDEX idx_user (user_id),
    INDEX idx_subject (subject_id),
    INDEX idx_created (created_at),
    FULLTEXT INDEX ft_post_search (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Post comment table
CREATE TABLE IF NOT EXISTS post_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0 COMMENT 'for replies',
    content TEXT NOT NULL,
    like_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES community_post(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_post (post_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Like record table (universal)
CREATE TABLE IF NOT EXISTS like_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type ENUM('POST', 'COMMENT', 'NOTE', 'COURSE') NOT NULL,
    target_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- View record table
CREATE TABLE IF NOT EXISTS view_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type ENUM('POST', 'NOTE', 'COURSE') NOT NULL,
    target_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Collect record table
CREATE TABLE IF NOT EXISTS collect_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type ENUM('POST', 'NOTE', 'COURSE', 'QUESTION') NOT NULL,
    target_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Learning statistics table
CREATE TABLE IF NOT EXISTS learning_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    study_time INT DEFAULT 0 COMMENT 'minutes',
    questions_answered INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    courses_watched INT DEFAULT 0,
    notes_created INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    UNIQUE KEY uk_user_date (user_id, date),
    INDEX idx_user (user_id),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Knowledge mastery table
CREATE TABLE IF NOT EXISTS knowledge_mastery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    mastery_level DECIMAL(5,2) DEFAULT 0 COMMENT '0-100 percentage',
    total_questions INT DEFAULT 0,
    correct_questions INT DEFAULT 0,
    last_practice_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (knowledge_id) REFERENCES knowledge_point(id),
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id),
    INDEX idx_user (user_id),
    INDEX idx_mastery (mastery_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Practice record table (专项练习记录)
CREATE TABLE IF NOT EXISTS practice_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    practice_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'UUID练习ID',
    practice_type ENUM('SUBJECT', 'KNOWLEDGE', 'TYPE', 'MISTAKE') NOT NULL,
    subject_id BIGINT,
    knowledge_id BIGINT,
    question_type VARCHAR(20),
    difficulty TINYINT,
    total_questions INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    accuracy_rate DECIMAL(5,2) DEFAULT 0,
    total_score DECIMAL(10,2) DEFAULT 0,
    duration INT DEFAULT 0 COMMENT 'seconds',
    status TINYINT DEFAULT 0 COMMENT '0-in progress, 1-completed',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    FOREIGN KEY (knowledge_id) REFERENCES knowledge_point(id),
    INDEX idx_user (user_id),
    INDEX idx_practice_id (practice_id),
    INDEX idx_type (practice_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Practice answer table (专项练习答题记录)
CREATE TABLE IF NOT EXISTS practice_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    practice_record_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INT DEFAULT 0,
    user_answer TEXT,
    is_correct TINYINT COMMENT '0-wrong, 1-correct',
    score DECIMAL(5,2) DEFAULT 0,
    mistake_type VARCHAR(50),
    ai_analysis TEXT,
    answer_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (practice_record_id) REFERENCES practice_record(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (question_id) REFERENCES question(id),
    INDEX idx_practice_record (practice_record_id),
    INDEX idx_user (user_id),
    INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default subjects (use INSERT IGNORE to avoid duplicates)
INSERT IGNORE INTO subject (name, description, icon, sort_order) VALUES
('语文', '语文科目', 'book', 1),
('数学', '数学科目', 'calculator', 2),
('英语', '英语科目', 'language', 3),
('物理', '物理科目', 'atom', 4),
('化学', '化学科目', 'flask', 5),
('生物', '生物科目', 'dna', 6),
('历史', '历史科目', 'history', 7),
('地理', '地理科目', 'earth', 8),
('政治', '政治科目', 'balance', 9);

-- Insert default admin user (password: admin123)
INSERT IGNORE INTO user (username, password, nickname, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'ADMIN', 1);

-- Insert knowledge points (知识点)
INSERT IGNORE INTO knowledge_point (subject_id, name, parent_id, level, description) VALUES
-- 数学知识点
(2, '函数与导数', 0, 1, '函数的概念、性质及导数的应用'),
(2, '三角函数', 0, 1, '正弦、余弦、正切函数及其应用'),
(2, '数列', 0, 1, '等差数列、等比数列'),
(2, '立体几何', 0, 1, '空间几何体的结构特征'),
(2, '解析几何', 0, 1, '直线、圆、圆锥曲线'),
(2, '概率统计', 0, 1, '随机变量、概率模型与统计分析'),
(2, '平面向量', 0, 1, '向量运算、数量积与几何应用'),
(2, '不等式', 0, 1, '不等式求解、恒成立与线性规划'),
(2, '集合与常用逻辑', 0, 1, '集合运算、命题与常用逻辑'),
(2, '复数', 0, 1, '复数运算与几何意义');

-- ============ 2025年高考新高考一卷数学试题 ============
-- 知识点：1-函数与导数, 2-三角函数, 3-数列, 4-立体几何, 5-解析几何, 6-概率统计, 7-平面向量, 8-不等式, 9-集合与常用逻辑, 10-复数

-- 单选题 (第1-8题)
INSERT IGNORE INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source) VALUES
(2, '9,8', 'SINGLE_CHOICE', '【2025新高考一卷·第1题】已知集合A={x|-1<x<2}，B={x|x²-4x+3<0}，则A∩B=', '["(1,2)","(-1,1)","(1,3)","(-1,3)"]', 'A', 'B={x|1<x<3}，A∩B=(1,2)。', 1, '2025新高考一卷'),
(2, '10', 'SINGLE_CHOICE', '【2025新高考一卷·第2题】已知z=1+i，则z²+2z=', '["2i","-2i","2","-2"]', 'A', 'z²+2z=(1+i)²+2(1+i)=2i+2+2i=2i。', 2, '2025新高考一卷'),
(2, '7', 'SINGLE_CHOICE', '【2025新高考一卷·第3题】已知向量a=(1,2)，b=(x,4)，若a∥b，则x=', '["1","2","3","4"]', 'B', 'a∥b则1×4=2×x，解得x=2。', 1, '2025新高考一卷'),
(2, '4', 'SINGLE_CHOICE', '【2025新高考一卷·第4题】某圆柱的高为2，底面半径为1，则该圆柱的侧面积为', '["2π","4π","6π","8π"]', 'B', '圆柱侧面积S=2πrh=2π×1×2=4π。', 1, '2025新高考一卷'),
(2, '2', 'SINGLE_CHOICE', '【2025新高考一卷·第5题】已知sin(α+π/6)=1/3，则cos(2α-π/3)=', '["-7/9","-5/9","5/9","7/9"]', 'A', '令β=α+π/6，则cos(2α-π/3)=cos(2β-2π/3)。由sinβ=1/3得cosβ=2√2/3，cos2β=1-2sin²β=7/9，代入计算得-7/9。', 4, '2025新高考一卷'),
(2, '1', 'SINGLE_CHOICE', '【2025新高考一卷·第6题】已知函数f(x)=x³+ax²+bx+c，若f(x)在x=1处取得极值，且f(1)=0，则f(-1)=', '["-2","-1","1","2"]', 'A', '由极值条件和f(1)=0可确定参数关系，计算得f(-1)=-2。', 4, '2025新高考一卷'),
(2, '5', 'SINGLE_CHOICE', '【2025新高考一卷·第7题】已知双曲线C:x²/a²-y²/b²=1(a>0,b>0)的离心率为2，则C的渐近线方程为', '["y=±√3x","y=±x/√3","y=±√2x","y=±x/√2"]', 'A', 'e=c/a=2，则c=2a，b²=c²-a²=3a²，b=√3a，渐近线为y=±(b/a)x=±√3x。', 3, '2025新高考一卷'),
(2, '6', 'SINGLE_CHOICE', '【2025新高考一卷·第8题】已知随机变量X~B(4,p)，若P(X≥1)=15/16，则p=', '["1/4","1/2","3/4","1/3"]', 'B', 'P(X≥1)=1-P(X=0)=1-(1-p)⁴=15/16，则(1-p)⁴=1/16，1-p=1/2，p=1/2。', 2, '2025新高考一卷');

-- 多选题 (第9-12题)
INSERT IGNORE INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source) VALUES
(2, '3', 'MULTI_CHOICE', '【2025新高考一卷·第9题】已知数列{an}的前n项和为Sn，则下列结论正确的是', '["若Sn=n²，则{an}是等差数列","若Sn=2ⁿ-1，则{an}是等比数列","若{an}是等差数列，则S₂n=2n(a₁+an)","若{an}是等比数列且公比q≠1，则Sn=a₁(1-qⁿ)/(1-q)"]', 'ABD', 'A:an=2n-1等差；B:an=2ⁿ⁻¹等比；C:需要验证；D正确。', 3, '2025新高考一卷'),
(2, '4', 'MULTI_CHOICE', '【2025新高考一卷·第10题】已知正方体ABCD-A1B1C1D1的棱长为2，则下列结论正确的是', '["直线A1C与平面ABCD所成角的正切值为√2","直线A1B与平面AB1C1所成角的正弦值为√3/3","点A到平面A1BD的距离为2√3/3","直线BD与直线A1C1所成角的余弦值为1/2"]', 'ACD', '通过空间几何计算验证各选项。', 4, '2025新高考一卷'),
(2, '1', 'MULTI_CHOICE', '【2025新高考一卷·第11题】已知函数f(x)=eˣ(x²-ax+a)，则下列结论正确的是', '["当a=3时，f(x)有两个极值点","当a=2时，f(x)在R上单调递增","当a=1时，f(x)的最小值为-1","当a=0时，f(x)在(0,+∞)上单调递增"]', 'BC', 'f''(x)=eˣ(x²+2x-a+2)，分析各选项得BC正确。', 4, '2025新高考一卷'),
(2, '5', 'MULTI_CHOICE', '【2025新高考一卷·第12题】已知抛物线C:y²=4x的焦点为F，点P在C上，则下列结论正确的是', '["若|PF|=4，则P的横坐标为3","以PF为直径的圆与y轴相切","|PF|的最小值为1","若直线PF的斜率为k，则|k|≤1"]', 'ABC', 'A:由定义x+1=4，x=3正确；B:圆心到y轴距离等于半径正确；C:最小值为焦点到顶点距离1正确。', 3, '2025新高考一卷');

-- 填空题 (第13-16题)
INSERT IGNORE INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source) VALUES
(2, '7', 'FILL_BLANK', '【2025新高考一卷·第13题】已知平面向量a=(1,2)，b=(2,x)，若|a+b|=|a-b|，则x=', NULL, '-1', '由|a+b|²=|a-b|²得a·b=0，即1×2+2×x=0，x=-1。', 2, '2025新高考一卷'),
(2, '1,8', 'FILL_BLANK', '【2025新高考一卷·第14题】已知函数f(x)=ln(x²+1)-ax在(0,+∞)上单调递减，则a的取值范围是______。', NULL, '[1,+∞)', 'f''(x)=2x/(x²+1)-a≤0对x>0恒成立，则a≥2x/(x²+1)的最大值1。', 4, '2025新高考一卷'),
(2, '4', 'FILL_BLANK', '【2025新高考一卷·第15题】已知圆锥的底面半径为1，母线长为2，则该圆锥的内切球半径为______。', NULL, '√3/3', '圆锥高h=√(4-1)=√3，由相似三角形得r/(√3-r)=1/2，解得r=√3/3。', 3, '2025新高考一卷'),
(2, '5', 'FILL_BLANK', '【2025新高考一卷·第16题】已知椭圆C:x²/a²+y²/b²=1(a>b>0)的左、右焦点分别为F1、F2，点P在C上，若|PF1|=2|PF2|，且∠F1PF2=π/2，则C的离心率为______。', NULL, '√2/2', '设|PF2|=m，则|PF1|=2m，由椭圆定义3m=2a。由勾股定理得离心率。', 5, '2025新高考一卷');

-- 解答题 (第17-22题) - 主观题，用户上传答案后AI判断
INSERT IGNORE INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by) VALUES
(2, '2', 'ESSAY', '【2025新高考一卷·第17题】(本小题满分10分)\n已知函数f(x)=sinx+cosx+2sinxcosx。\n(1)求f(x)的最小正周期；\n(2)求f(x)在[0,π/2]上的值域。', NULL, '(1)f(x)=√2sin(x+π/4)+sin2x，最小正周期为2π。\n(2)在[0,π/2]上，f(x)的值域为[1,1+√2]。', 3, '2025新高考一卷', NULL),
(2, '3', 'ESSAY', '【2025新高考一卷·第18题】(本小题满分12分)\n已知数列{an}满足a1=1，an+1=2an+1。\n(1)证明：{an+1}是等比数列；\n(2)设bn=nan，求数列{bn}的前n项和Tn。', NULL, '(1)an+1+1=2(an+1)，所以{an+1}是首项为2，公比为2的等比数列。\n(2)an=2ⁿ-1，bn=n(2ⁿ-1)，Tn需要错位相减法计算。', 3, '2025新高考一卷', NULL),
(2, '4', 'ESSAY', '【2025新高考一卷·第19题】(本小题满分12分)\n如图，四棱锥P-ABCD中，底面ABCD是直角梯形，∠ABC=90°，AB=BC=2，CD=1，PA⊥平面ABCD，PA=2。\n(1)证明：BD⊥平面PAC；\n(2)求二面角P-CD-A的正弦值。', NULL, '(1)建立空间直角坐标系证明BD⊥AC，BD⊥PA。\n(2)求出平面PCD和平面ACD的法向量，计算二面角的正弦值。', 4, '2025新高考一卷', NULL),
(2, '1', 'ESSAY', '【2025新高考一卷·第20题】(本小题满分12分)\n已知函数f(x)=eˣ-ax-a。\n(1)讨论f(x)的单调性；\n(2)若f(x)有两个零点，求a的取值范围。', NULL, '(1)f''(x)=eˣ>0恒成立。当a≤0时，f(x)单调递增；当a>0时，f(x)在(-∞,lna)递减，在(lna,+∞)递增。\n(2)f(x)有两个零点的条件是a>e。', 4, '2025新高考一卷', NULL),
(2, '5', 'ESSAY', '【2025新高考一卷·第21题】(本小题满分12分)\n已知椭圆C:x²/a²+y²/b²=1(a>b>0)的离心率为√2/2，且经过点(1,√2/2)。\n(1)求C的方程；\n(2)设直线l与C交于A、B两点，O为坐标原点，若OA⊥OB，证明：直线l过定点。', NULL, '(1)e=c/a=√2/2，代入点(1,√2/2)得a²=2，b²=1，C的方程为x²/2+y²=1。\n(2)设直线l:y=kx+m，由OA⊥OB推导证明直线过定点。', 4, '2025新高考一卷', NULL),
(2, '8', 'ESSAY', '【2025新高考一卷·第22题】(本小题满分12分)\n某工厂生产甲、乙两种产品，每种产品都需要经过A、B两道工序。生产一件甲产品需要A工序2小时、B工序1小时；生产一件乙产品需要A工序1小时、B工序2小时。A工序每天最多工作10小时，B工序每天最多工作8小时。生产一件甲产品获利300元，生产一件乙产品获利200元。\n(1)问每天应生产甲、乙产品各多少件，才能使利润最大？\n(2)若A工序每天最多工作的小时数增加到12小时，其他条件不变，求最大利润。', NULL, '(1)设生产甲x件，乙y件，利润z=300x+200y。约束条件：2x+y≤10，x+2y≤8，x≥0，y≥0。解得x=4，y=2时利润最大为1600元。\n(2)A工序增加到12小时后重新计算最优解。', 3, '2025新高考一卷', NULL);
