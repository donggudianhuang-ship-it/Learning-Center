-- Deduplicate 2025 exam questions while preserving dependent records.
-- Scope: only questions whose source starts with '2025'.

USE smart_learning;

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_duplicate_questions AS
SELECT q.id AS duplicate_id, kept.keep_id
FROM question q
JOIN (
    SELECT subject_id, source, content, MIN(id) AS keep_id
    FROM question
    WHERE source LIKE '2025%'
    GROUP BY subject_id, source, content
    HAVING COUNT(*) > 1
) kept ON q.subject_id = kept.subject_id
    AND q.source = kept.source
    AND q.content = kept.content
    AND q.id <> kept.keep_id;

-- Exam-paper links are unique by (exam_id, question_id).
DELETE eq
FROM exam_question eq
JOIN tmp_duplicate_questions d ON eq.question_id = d.duplicate_id
JOIN exam_question kept_eq
    ON kept_eq.exam_id = eq.exam_id
    AND kept_eq.question_id = d.keep_id;

UPDATE exam_question eq
JOIN tmp_duplicate_questions d ON eq.question_id = d.duplicate_id
SET eq.question_id = d.keep_id;

-- Answer records can be repointed directly.
UPDATE answer_record ar
JOIN tmp_duplicate_questions d ON ar.question_id = d.duplicate_id
SET ar.question_id = d.keep_id;

UPDATE practice_answer pa
JOIN tmp_duplicate_questions d ON pa.question_id = d.duplicate_id
SET pa.question_id = d.keep_id;

-- Mistake-book rows are unique by (user_id, question_id), so merge useful state first.
UPDATE mistake_book kept_mb
JOIN tmp_duplicate_questions d ON kept_mb.question_id = d.keep_id
JOIN mistake_book dup_mb
    ON dup_mb.user_id = kept_mb.user_id
    AND dup_mb.question_id = d.duplicate_id
SET kept_mb.review_count = GREATEST(kept_mb.review_count, dup_mb.review_count),
    kept_mb.mastery_level = GREATEST(kept_mb.mastery_level, dup_mb.mastery_level),
    kept_mb.last_review_at = CASE
        WHEN kept_mb.last_review_at IS NULL THEN dup_mb.last_review_at
        WHEN dup_mb.last_review_at IS NULL THEN kept_mb.last_review_at
        ELSE GREATEST(kept_mb.last_review_at, dup_mb.last_review_at)
    END,
    kept_mb.next_review_date = CASE
        WHEN kept_mb.next_review_date IS NULL THEN dup_mb.next_review_date
        WHEN dup_mb.next_review_date IS NULL THEN kept_mb.next_review_date
        ELSE LEAST(kept_mb.next_review_date, dup_mb.next_review_date)
    END,
    kept_mb.note = COALESCE(NULLIF(kept_mb.note, ''), dup_mb.note);

DELETE mb
FROM mistake_book mb
JOIN tmp_duplicate_questions d ON mb.question_id = d.duplicate_id
JOIN mistake_book kept_mb
    ON kept_mb.user_id = mb.user_id
    AND kept_mb.question_id = d.keep_id;

UPDATE mistake_book mb
JOIN tmp_duplicate_questions d ON mb.question_id = d.duplicate_id
SET mb.question_id = d.keep_id;

-- Collections are unique by (user_id, target_type, target_id).
UPDATE collect_record kept_cr
JOIN tmp_duplicate_questions d ON kept_cr.target_type = 'QUESTION'
    AND kept_cr.target_id = d.keep_id
JOIN collect_record dup_cr
    ON dup_cr.user_id = kept_cr.user_id
    AND dup_cr.target_type = 'QUESTION'
    AND dup_cr.target_id = d.duplicate_id
SET kept_cr.created_at = LEAST(kept_cr.created_at, dup_cr.created_at);

DELETE cr
FROM collect_record cr
JOIN tmp_duplicate_questions d
    ON cr.target_type = 'QUESTION'
    AND cr.target_id = d.duplicate_id
JOIN collect_record kept_cr
    ON kept_cr.user_id = cr.user_id
    AND kept_cr.target_type = 'QUESTION'
    AND kept_cr.target_id = d.keep_id;

UPDATE collect_record cr
JOIN tmp_duplicate_questions d
    ON cr.target_type = 'QUESTION'
    AND cr.target_id = d.duplicate_id
SET cr.target_id = d.keep_id;

DELETE q
FROM question q
JOIN tmp_duplicate_questions d ON q.id = d.duplicate_id;

DROP TEMPORARY TABLE tmp_duplicate_questions;

COMMIT;
