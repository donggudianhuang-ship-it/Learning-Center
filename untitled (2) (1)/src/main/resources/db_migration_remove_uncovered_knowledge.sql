-- Remove knowledge points that are not referenced by any current question.
-- Dependent learning records tied to those knowledge points are removed first.

USE smart_learning;

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_uncovered_knowledge AS
SELECT kp.id
FROM knowledge_point kp
WHERE NOT EXISTS (
    SELECT 1
    FROM question q
    WHERE q.knowledge_ids IS NOT NULL
      AND q.knowledge_ids <> ''
      AND FIND_IN_SET(kp.id, q.knowledge_ids)
);

CREATE TEMPORARY TABLE tmp_uncovered_questions AS
SELECT DISTINCT q.id
FROM question q
JOIN tmp_uncovered_knowledge uk ON FIND_IN_SET(uk.id, q.knowledge_ids);

DELETE pa
FROM practice_answer pa
JOIN tmp_uncovered_questions uq ON uq.id = pa.question_id;

DELETE ar
FROM answer_record ar
JOIN tmp_uncovered_questions uq ON uq.id = ar.question_id;

DELETE mb
FROM mistake_book mb
JOIN tmp_uncovered_questions uq ON uq.id = mb.question_id;

DELETE pa
FROM practice_answer pa
JOIN practice_record pr ON pr.id = pa.practice_record_id
JOIN tmp_uncovered_knowledge uk ON uk.id = pr.knowledge_id;

DELETE pr
FROM practice_record pr
JOIN tmp_uncovered_knowledge uk ON uk.id = pr.knowledge_id;

DELETE km
FROM knowledge_mastery km
JOIN tmp_uncovered_knowledge uk ON uk.id = km.knowledge_id;

DELETE kp
FROM knowledge_point kp
JOIN tmp_uncovered_knowledge uk ON uk.id = kp.id;

DROP TEMPORARY TABLE tmp_uncovered_questions;
DROP TEMPORARY TABLE tmp_uncovered_knowledge;

COMMIT;
