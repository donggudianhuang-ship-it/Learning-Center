-- Clean legacy system-imported questions and repair 2025 Gaokao math knowledge tags.
-- Safe scope: this removes only questions whose source is exactly '系统导入'.

USE smart_learning;

CREATE TEMPORARY TABLE tmp_system_questions AS
SELECT id FROM question WHERE source = '系统导入';

DELETE eq FROM exam_question eq
JOIN tmp_system_questions tq ON eq.question_id = tq.id;

DELETE pa FROM practice_answer pa
JOIN tmp_system_questions tq ON pa.question_id = tq.id;

DELETE ar FROM answer_record ar
JOIN tmp_system_questions tq ON ar.question_id = tq.id;

DELETE mb FROM mistake_book mb
JOIN tmp_system_questions tq ON mb.question_id = tq.id;

DELETE q FROM question q
JOIN tmp_system_questions tq ON q.id = tq.id;

DROP TEMPORARY TABLE tmp_system_questions;

SET @math_subject_id := (SELECT id FROM subject WHERE name = '数学' LIMIT 1);

INSERT INTO knowledge_point (subject_id, name, parent_id, level, description)
SELECT @math_subject_id, '概率统计', 0, 1, '随机变量、概率模型与统计分析'
WHERE @math_subject_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_point
      WHERE subject_id = @math_subject_id AND name = '概率统计'
  );

INSERT INTO knowledge_point (subject_id, name, parent_id, level, description)
SELECT @math_subject_id, '平面向量', 0, 1, '向量运算、数量积与几何应用'
WHERE @math_subject_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_point
      WHERE subject_id = @math_subject_id AND name = '平面向量'
  );

INSERT INTO knowledge_point (subject_id, name, parent_id, level, description)
SELECT @math_subject_id, '不等式', 0, 1, '不等式求解、恒成立与线性规划'
WHERE @math_subject_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_point
      WHERE subject_id = @math_subject_id AND name = '不等式'
  );

INSERT INTO knowledge_point (subject_id, name, parent_id, level, description)
SELECT @math_subject_id, '集合与常用逻辑', 0, 1, '集合运算、命题与常用逻辑'
WHERE @math_subject_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_point
      WHERE subject_id = @math_subject_id AND name = '集合与常用逻辑'
  );

INSERT INTO knowledge_point (subject_id, name, parent_id, level, description)
SELECT @math_subject_id, '复数', 0, 1, '复数运算与几何意义'
WHERE @math_subject_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_point
      WHERE subject_id = @math_subject_id AND name = '复数'
  );

UPDATE question
SET knowledge_ids = (
    SELECT GROUP_CONCAT(kp.id ORDER BY FIELD(kp.name, '集合与常用逻辑', '不等式') SEPARATOR ',')
    FROM knowledge_point kp
    WHERE kp.subject_id = @math_subject_id AND kp.name IN ('集合与常用逻辑', '不等式')
)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第1题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '复数' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第2题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '平面向量' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第3题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '立体几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第4题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '三角函数' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第5题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '函数与导数' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第6题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '解析几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第7题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '概率统计' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第8题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '数列' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第9题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '立体几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第10题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '函数与导数' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第11题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '解析几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第12题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '平面向量' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第13题】%';

UPDATE question
SET knowledge_ids = (
    SELECT GROUP_CONCAT(kp.id ORDER BY FIELD(kp.name, '函数与导数', '不等式') SEPARATOR ',')
    FROM knowledge_point kp
    WHERE kp.subject_id = @math_subject_id AND kp.name IN ('函数与导数', '不等式')
)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第14题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '立体几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第15题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '解析几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第16题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '三角函数' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第17题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '数列' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第18题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '立体几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第19题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '函数与导数' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第20题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '解析几何' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第21题】%';

UPDATE question
SET knowledge_ids = (SELECT CAST(kp.id AS CHAR) FROM knowledge_point kp WHERE kp.subject_id = @math_subject_id AND kp.name = '不等式' LIMIT 1)
WHERE source = '2025新高考一卷' AND content LIKE '【2025新高考一卷·第22题】%';
