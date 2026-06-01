-- Repair demo learning data:
-- 1) merge duplicated subjects and knowledge points,
-- 2) reset the current demo user's learning history,
-- 3) ensure the 2025 math paper contains questions 1-22,
-- 4) rebuild the exam-question links.

USE smart_learning;

START TRANSACTION;

-- Merge duplicated subjects by name, keeping the smallest id for each name.
CREATE TEMPORARY TABLE tmp_subject_keep AS
SELECT s.id AS duplicate_id, kept.keep_id
FROM subject s
JOIN (
    SELECT name, MIN(id) AS keep_id
    FROM subject
    GROUP BY name
    HAVING COUNT(*) > 1
) kept ON kept.name = s.name AND kept.keep_id <> s.id;

UPDATE knowledge_point kp
JOIN tmp_subject_keep sk ON sk.duplicate_id = kp.subject_id
SET kp.subject_id = sk.keep_id;

UPDATE question q
JOIN tmp_subject_keep sk ON sk.duplicate_id = q.subject_id
SET q.subject_id = sk.keep_id;

UPDATE exam_paper ep
JOIN tmp_subject_keep sk ON sk.duplicate_id = ep.subject_id
SET ep.subject_id = sk.keep_id;

UPDATE course c
JOIN tmp_subject_keep sk ON sk.duplicate_id = c.subject_id
SET c.subject_id = sk.keep_id;

UPDATE study_note sn
JOIN tmp_subject_keep sk ON sk.duplicate_id = sn.subject_id
SET sn.subject_id = sk.keep_id;

UPDATE community_post cp
JOIN tmp_subject_keep sk ON sk.duplicate_id = cp.subject_id
SET cp.subject_id = sk.keep_id;

UPDATE practice_record pr
JOIN tmp_subject_keep sk ON sk.duplicate_id = pr.subject_id
SET pr.subject_id = sk.keep_id;

DELETE s
FROM subject s
JOIN tmp_subject_keep sk ON sk.duplicate_id = s.id;

DROP TEMPORARY TABLE tmp_subject_keep;

-- Merge duplicated knowledge points by (subject_id, name).
CREATE TEMPORARY TABLE tmp_knowledge_keep AS
SELECT kp.id AS duplicate_id, kept.keep_id
FROM knowledge_point kp
JOIN (
    SELECT subject_id, name, MIN(id) AS keep_id
    FROM knowledge_point
    GROUP BY subject_id, name
    HAVING COUNT(*) > 1
) kept ON kept.subject_id = kp.subject_id
    AND kept.name = kp.name
    AND kept.keep_id <> kp.id;

CREATE TEMPORARY TABLE tmp_knowledge_merge_ids (
    id BIGINT PRIMARY KEY
);

INSERT IGNORE INTO tmp_knowledge_merge_ids (id)
SELECT duplicate_id FROM tmp_knowledge_keep;

INSERT IGNORE INTO tmp_knowledge_merge_ids (id)
SELECT keep_id FROM tmp_knowledge_keep;

-- Rebuild comma-separated question.knowledge_ids after applying the duplicate-id map.
CREATE TEMPORARY TABLE tmp_question_knowledge_tokens AS
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 80
)
SELECT parsed.question_id,
       COALESCE(kk.keep_id, parsed.raw_knowledge_id) AS knowledge_id,
       parsed.position
FROM (
    SELECT q.id AS question_id,
           CAST(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(q.knowledge_ids, ',', seq.n), ',', -1)) AS UNSIGNED) AS raw_knowledge_id,
           seq.n AS position
    FROM question q
    JOIN seq ON seq.n <= 1 + LENGTH(q.knowledge_ids) - LENGTH(REPLACE(q.knowledge_ids, ',', ''))
    WHERE q.knowledge_ids IS NOT NULL
      AND q.knowledge_ids <> ''
      AND TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(q.knowledge_ids, ',', seq.n), ',', -1)) <> ''
) parsed
LEFT JOIN tmp_knowledge_keep kk ON kk.duplicate_id = parsed.raw_knowledge_id;

CREATE TEMPORARY TABLE tmp_question_knowledge_rebuilt AS
SELECT question_id,
       GROUP_CONCAT(knowledge_id ORDER BY first_position SEPARATOR ',') AS new_knowledge_ids
FROM (
    SELECT question_id, knowledge_id, MIN(position) AS first_position
    FROM tmp_question_knowledge_tokens
    GROUP BY question_id, knowledge_id
) deduped
GROUP BY question_id;

UPDATE question q
JOIN tmp_question_knowledge_rebuilt rebuilt ON rebuilt.question_id = q.id
SET q.knowledge_ids = rebuilt.new_knowledge_ids;

CREATE TEMPORARY TABLE tmp_mastery_rollup AS
SELECT km.user_id,
       COALESCE(kk.keep_id, km.knowledge_id) AS knowledge_id,
       SUM(COALESCE(km.total_questions, 0)) AS total_questions,
       SUM(COALESCE(km.correct_questions, 0)) AS correct_questions,
       MAX(km.last_practice_at) AS last_practice_at
FROM knowledge_mastery km
JOIN tmp_knowledge_merge_ids merge_ids ON merge_ids.id = km.knowledge_id
LEFT JOIN tmp_knowledge_keep kk ON kk.duplicate_id = km.knowledge_id
GROUP BY km.user_id, COALESCE(kk.keep_id, km.knowledge_id);

DELETE km
FROM knowledge_mastery km
JOIN tmp_knowledge_merge_ids merge_ids ON merge_ids.id = km.knowledge_id;

INSERT INTO knowledge_mastery (user_id, knowledge_id, mastery_level, total_questions, correct_questions, last_practice_at, created_at, updated_at)
SELECT user_id,
       knowledge_id,
       CASE WHEN total_questions > 0 THEN ROUND(correct_questions * 100.0 / total_questions, 1) ELSE 0 END,
       total_questions,
       correct_questions,
       last_practice_at,
       NOW(),
       NOW()
FROM tmp_mastery_rollup;

UPDATE practice_record pr
JOIN tmp_knowledge_keep kk ON kk.duplicate_id = pr.knowledge_id
SET pr.knowledge_id = kk.keep_id;

UPDATE study_note sn
JOIN tmp_knowledge_keep kk ON kk.duplicate_id = sn.knowledge_id
SET sn.knowledge_id = kk.keep_id;

DELETE kp
FROM knowledge_point kp
JOIN tmp_knowledge_keep kk ON kk.duplicate_id = kp.id;

DROP TEMPORARY TABLE tmp_mastery_rollup;
DROP TEMPORARY TABLE tmp_question_knowledge_rebuilt;
DROP TEMPORARY TABLE tmp_question_knowledge_tokens;
DROP TEMPORARY TABLE tmp_knowledge_merge_ids;
DROP TEMPORARY TABLE tmp_knowledge_keep;

-- Normalize 2025 math question knowledge ids to the remaining canonical ids.
SET @math_subject_id := (SELECT id FROM subject WHERE name = '数学' ORDER BY id LIMIT 1);
SET @kp_func := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '函数与导数' ORDER BY id LIMIT 1);
SET @kp_tri := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '三角函数' ORDER BY id LIMIT 1);
SET @kp_seq := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '数列' ORDER BY id LIMIT 1);
SET @kp_solid := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '立体几何' ORDER BY id LIMIT 1);
SET @kp_geo := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '解析几何' ORDER BY id LIMIT 1);
SET @kp_prob := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '概率统计' ORDER BY id LIMIT 1);
SET @kp_vec := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '平面向量' ORDER BY id LIMIT 1);
SET @kp_ineq := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '不等式' ORDER BY id LIMIT 1);
SET @kp_logic := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '集合与常用逻辑' ORDER BY id LIMIT 1);
SET @kp_complex := (SELECT id FROM knowledge_point WHERE subject_id = @math_subject_id AND name = '复数' ORDER BY id LIMIT 1);

UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CONCAT(@kp_logic, ',', @kp_ineq)
WHERE source LIKE '2025%' AND content LIKE '%第1题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_complex AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第2题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_vec AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第3题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_solid AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第4题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_tri AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第5题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_func AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第6题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_geo AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第7题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_prob AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第8题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_seq AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第9题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_solid AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第10题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_func AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第11题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_geo AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第12题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_vec AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第13题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CONCAT(@kp_func, ',', @kp_ineq)
WHERE source LIKE '2025%' AND content LIKE '%第14题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_solid AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第15题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_geo AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第16题】%';

-- Add missing essay questions 17-22.
INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by)
SELECT @math_subject_id, CAST(@kp_tri AS CHAR), 'ESSAY',
       '【2025新高考一卷·第17题】(本小题满分10分)
已知函数f(x)=sinx+cosx+2sinxcosx。
(1)求f(x)的最小正周期；
(2)求f(x)在[0,π/2]上的值域。',
       NULL,
       '(1)f(x)=√2sin(x+π/4)+sin2x，最小正周期为2π。
(2)在[0,π/2]上，f(x)的值域为[1,1+√2]。',
       '先化简三角表达式，再结合区间讨论函数值域。', 3, '2025新高考一卷', NULL
WHERE NOT EXISTS (SELECT 1 FROM question WHERE source LIKE '2025%' AND content LIKE '%第17题】%');

INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by)
SELECT @math_subject_id, CAST(@kp_seq AS CHAR), 'ESSAY',
       '【2025新高考一卷·第18题】(本小题满分12分)
已知数列{an}满足a1=1，an+1=2an+1。
(1)证明：{an+1}是等比数列；
(2)设bn=nan，求数列{bn}的前n项和Tn。',
       NULL,
       '(1)an+1+1=2(an+1)，所以{an+1}是首项为2，公比为2的等比数列。
(2)an=2ⁿ-1，bn=n(2ⁿ-1)，Tn需要错位相减法计算。',
       '关键是构造等比数列，并使用错位相减处理n·2ⁿ型求和。', 3, '2025新高考一卷', NULL
WHERE NOT EXISTS (SELECT 1 FROM question WHERE source LIKE '2025%' AND content LIKE '%第18题】%');

INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by)
SELECT @math_subject_id, CAST(@kp_solid AS CHAR), 'ESSAY',
       '【2025新高考一卷·第19题】(本小题满分12分)
如图，四棱锥P-ABCD中，底面ABCD是直角梯形，∠ABC=90°，AB=BC=2，CD=1，PA⊥平面ABCD，PA=2。
(1)证明：BD⊥平面PAC；
(2)求二面角P-CD-A的正弦值。',
       NULL,
       '(1)建立空间直角坐标系证明BD⊥AC，BD⊥PA。
(2)求出平面PCD和平面ACD的法向量，计算二面角的正弦值。',
       '用线面垂直判定和空间向量法处理二面角。', 4, '2025新高考一卷', NULL
WHERE NOT EXISTS (SELECT 1 FROM question WHERE source LIKE '2025%' AND content LIKE '%第19题】%');

INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by)
SELECT @math_subject_id, CAST(@kp_func AS CHAR), 'ESSAY',
       '【2025新高考一卷·第20题】(本小题满分12分)
已知函数f(x)=eˣ-ax-a。
(1)讨论f(x)的单调性；
(2)若f(x)有两个零点，求a的取值范围。',
       NULL,
       '(1)根据导数和参数a讨论单调区间。
(2)结合极值与端点趋势，得到f(x)有两个零点的参数范围。',
       '导数与零点问题，重点是分类讨论参数和极值条件。', 4, '2025新高考一卷', NULL
WHERE NOT EXISTS (SELECT 1 FROM question WHERE source LIKE '2025%' AND content LIKE '%第20题】%');

INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by)
SELECT @math_subject_id, CAST(@kp_geo AS CHAR), 'ESSAY',
       '【2025新高考一卷·第21题】(本小题满分12分)
已知椭圆C:x²/a²+y²/b²=1(a>b>0)的离心率为√2/2，且经过点(1,√2/2)。
(1)求C的方程；
(2)设直线l与C交于A、B两点，O为坐标原点，若OA⊥OB，证明：直线l过定点。',
       NULL,
       '(1)e=c/a=√2/2，代入点(1,√2/2)得a²=2，b²=1，C的方程为x²/2+y²=1。
(2)设直线l:y=kx+m，由OA⊥OB推导证明直线过定点。',
       '先求椭圆标准方程，再用韦达定理和垂直条件推定点。', 4, '2025新高考一卷', NULL
WHERE NOT EXISTS (SELECT 1 FROM question WHERE source LIKE '2025%' AND content LIKE '%第21题】%');

INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by)
SELECT @math_subject_id, CAST(@kp_ineq AS CHAR), 'ESSAY',
       '【2025新高考一卷·第22题】(本小题满分12分)
某工厂生产甲、乙两种产品，每种产品都需要经过A、B两道工序。生产一件甲产品需要A工序2小时、B工序1小时；生产一件乙产品需要A工序1小时、B工序2小时。A工序每天最多工作10小时，B工序每天最多工作8小时。生产一件甲产品获利300元，生产一件乙产品获利200元。
(1)问每天应生产甲、乙产品各多少件，才能使利润最大？
(2)若A工序每天最多工作的小时数增加到12小时，其他条件不变，求最大利润。',
       NULL,
       '(1)设生产甲x件，乙y件，利润z=300x+200y。约束条件：2x+y≤10，x+2y≤8，x≥0，y≥0。解得x=4，y=2时利润最大为1600元。
(2)A工序增加到12小时后重新计算最优解。',
       '线性规划应用题，重点是建立约束条件并比较可行域顶点。', 3, '2025新高考一卷', NULL
WHERE NOT EXISTS (SELECT 1 FROM question WHERE source LIKE '2025%' AND content LIKE '%第22题】%');

UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_tri AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第17题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_seq AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第18题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_solid AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第19题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_func AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第20题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_geo AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第21题】%';
UPDATE question SET subject_id = @math_subject_id, knowledge_ids = CAST(@kp_ineq AS CHAR)
WHERE source LIKE '2025%' AND content LIKE '%第22题】%';

-- Reset the current demo user's historical learning analytics.
SET @demo_user_id := (SELECT id FROM user WHERE username = 'jht' ORDER BY id LIMIT 1);

DELETE pa FROM practice_answer pa WHERE pa.user_id = @demo_user_id;
DELETE pr FROM practice_record pr WHERE pr.user_id = @demo_user_id;
DELETE ar FROM answer_record ar WHERE ar.user_id = @demo_user_id;
DELETE mb FROM mistake_book mb WHERE mb.user_id = @demo_user_id;
DELETE km FROM knowledge_mastery km WHERE km.user_id = @demo_user_id;
DELETE es FROM exam_submission es WHERE es.user_id = @demo_user_id;
DELETE ls FROM learning_stats ls WHERE ls.user_id = @demo_user_id;
DELETE cp FROM course_progress cp WHERE cp.user_id = @demo_user_id;

-- Rebuild the 2025 math exam links.
SET @admin_id := COALESCE(
    (SELECT id FROM user WHERE role = 'ADMIN' ORDER BY id LIMIT 1),
    (SELECT MIN(id) FROM user)
);
SET @exam_id := (SELECT id FROM exam_paper WHERE name = '2025新高考一卷数学演练' ORDER BY id LIMIT 1);

INSERT INTO exam_paper (name, subject_id, creator_id, total_score, duration, description, status, created_at)
SELECT '2025新高考一卷数学演练', @math_subject_id, @admin_id, 150, 120,
       '保留的2025新高考一卷数学题，按知识点完成标注后用于诊断练习',
       1, NOW()
WHERE @exam_id IS NULL;

SET @exam_id := (SELECT id FROM exam_paper WHERE name = '2025新高考一卷数学演练' ORDER BY id LIMIT 1);

DELETE eq
FROM exam_question eq
WHERE eq.exam_id = @exam_id;

INSERT INTO exam_question (exam_id, question_id, score, sort_order)
SELECT @exam_id,
       q.id,
       CASE
           WHEN q.content LIKE '%第1题】%' THEN 5
           WHEN q.content LIKE '%第2题】%' THEN 5
           WHEN q.content LIKE '%第3题】%' THEN 5
           WHEN q.content LIKE '%第4题】%' THEN 5
           WHEN q.content LIKE '%第5题】%' THEN 5
           WHEN q.content LIKE '%第6题】%' THEN 5
           WHEN q.content LIKE '%第7题】%' THEN 5
           WHEN q.content LIKE '%第8题】%' THEN 5
           WHEN q.content LIKE '%第9题】%' THEN 6
           WHEN q.content LIKE '%第10题】%' THEN 6
           WHEN q.content LIKE '%第11题】%' THEN 6
           WHEN q.content LIKE '%第12题】%' THEN 6
           WHEN q.content LIKE '%第13题】%' THEN 5
           WHEN q.content LIKE '%第14题】%' THEN 5
           WHEN q.content LIKE '%第15题】%' THEN 5
           WHEN q.content LIKE '%第16题】%' THEN 5
           ELSE 12
       END AS score,
       CASE
           WHEN q.content LIKE '%第1题】%' THEN 1
           WHEN q.content LIKE '%第2题】%' THEN 2
           WHEN q.content LIKE '%第3题】%' THEN 3
           WHEN q.content LIKE '%第4题】%' THEN 4
           WHEN q.content LIKE '%第5题】%' THEN 5
           WHEN q.content LIKE '%第6题】%' THEN 6
           WHEN q.content LIKE '%第7题】%' THEN 7
           WHEN q.content LIKE '%第8题】%' THEN 8
           WHEN q.content LIKE '%第9题】%' THEN 9
           WHEN q.content LIKE '%第10题】%' THEN 10
           WHEN q.content LIKE '%第11题】%' THEN 11
           WHEN q.content LIKE '%第12题】%' THEN 12
           WHEN q.content LIKE '%第13题】%' THEN 13
           WHEN q.content LIKE '%第14题】%' THEN 14
           WHEN q.content LIKE '%第15题】%' THEN 15
           WHEN q.content LIKE '%第16题】%' THEN 16
           WHEN q.content LIKE '%第17题】%' THEN 17
           WHEN q.content LIKE '%第18题】%' THEN 18
           WHEN q.content LIKE '%第19题】%' THEN 19
           WHEN q.content LIKE '%第20题】%' THEN 20
           WHEN q.content LIKE '%第21题】%' THEN 21
           WHEN q.content LIKE '%第22题】%' THEN 22
           ELSE 999
       END AS sort_order
FROM question q
WHERE q.source LIKE '2025%'
ORDER BY sort_order;

COMMIT;
