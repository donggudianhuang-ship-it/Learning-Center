USE smart_learning;

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_remove_science_questions AS
SELECT id
FROM question
WHERE source IN ('2025全国卷理综化学', '2025高考课标卷生物', '2025高考课标卷物理');

CREATE TEMPORARY TABLE tmp_remove_science_exams AS
SELECT id
FROM exam_paper
WHERE name IN ('2025全国卷理综化学真题训练', '2025高考课标卷生物真题训练', '2025高考课标卷物理真题训练');

CREATE TEMPORARY TABLE tmp_remove_science_subjects AS
SELECT id
FROM subject
WHERE name IN ('化学', '生物', '物理');

CREATE TEMPORARY TABLE tmp_remove_science_knowledge AS
SELECT kp.id
FROM knowledge_point kp
JOIN tmp_remove_science_subjects s ON s.id = kp.subject_id;

DELETE pa
FROM practice_answer pa
JOIN tmp_remove_science_questions q ON q.id = pa.question_id;

DELETE ar
FROM answer_record ar
JOIN tmp_remove_science_questions q ON q.id = ar.question_id;

DELETE mb
FROM mistake_book mb
JOIN tmp_remove_science_questions q ON q.id = mb.question_id;

DELETE eq
FROM exam_question eq
JOIN tmp_remove_science_questions q ON q.id = eq.question_id;

DELETE eq
FROM exam_question eq
JOIN tmp_remove_science_exams e ON e.id = eq.exam_id;

DELETE es
FROM exam_submission es
JOIN tmp_remove_science_exams e ON e.id = es.exam_id;

DELETE pr
FROM practice_record pr
LEFT JOIN tmp_remove_science_subjects s ON s.id = pr.subject_id
LEFT JOIN tmp_remove_science_knowledge kp ON kp.id = pr.knowledge_id
WHERE s.id IS NOT NULL OR kp.id IS NOT NULL;

DELETE km
FROM knowledge_mastery km
JOIN tmp_remove_science_knowledge kp ON kp.id = km.knowledge_id;

DELETE FROM exam_paper
WHERE id IN (SELECT id FROM tmp_remove_science_exams);

DELETE FROM question
WHERE id IN (SELECT id FROM tmp_remove_science_questions);

DELETE FROM knowledge_point
WHERE id IN (SELECT id FROM tmp_remove_science_knowledge);

DROP TEMPORARY TABLE tmp_remove_science_questions;
DROP TEMPORARY TABLE tmp_remove_science_exams;
DROP TEMPORARY TABLE tmp_remove_science_subjects;
DROP TEMPORARY TABLE tmp_remove_science_knowledge;

COMMIT;
