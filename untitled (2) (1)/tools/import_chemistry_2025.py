from __future__ import annotations

import importlib.util
from pathlib import Path


WORKSPACE = Path(__file__).resolve().parents[1]
COMMON_PATH = WORKSPACE / "tools" / "import_science_2025.py"
SQL_PATH = WORKSPACE / "src" / "main" / "resources" / "db_migration_import_chemistry_2025.sql"


spec = importlib.util.spec_from_file_location("science_import", COMMON_PATH)
common = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(common)


def parse_answer_details(answer_text: str) -> dict[int, dict[str, str]]:
    answers = common.parse_answers(answer_text, "chemistry")
    details: dict[int, dict[str, str]] = {}
    for number, answer in answers.items():
        if number in common.DOCS["chemistry"]["choice_numbers"]:
            details[number] = {
                "answer": answer,
                "analysis": f"参考答案：{answer}",
            }
        else:
            details[number] = {
                "answer": answer,
                "analysis": f"参考答案：\n{answer}",
            }
    return details


def build_chemistry_import() -> tuple[str, int, dict[str, int]]:
    config = common.DOCS["chemistry"]
    text, ole = common.extract_doc_text(config["path"])
    body, answer_text = common.split_body_and_answers(text, config["split_marker"])
    section_pos = body.find(config["question_section"])
    if section_pos >= 0:
        body = body[section_pos:]

    numbers = set(config["choice_numbers"]) | set(config["essay_numbers"])
    blocks = common.split_questions(body, numbers)
    answers = parse_answer_details(answer_text)
    image_urls = common.extract_pngs(ole, "chemistry")

    lines: list[str] = [
        "-- Import 2025 chemistry exam questions with images and answer analysis.",
        "USE smart_learning;",
        "START TRANSACTION;",
        "CREATE TEMPORARY TABLE tmp_chemistry_import_questions AS",
        "SELECT id FROM question WHERE source = '2025全国卷理综化学';",
        "CREATE TEMPORARY TABLE tmp_chemistry_import_exams AS",
        "SELECT id FROM exam_paper WHERE name = '2025全国卷理综化学真题训练';",
        "DELETE pa FROM practice_answer pa JOIN tmp_chemistry_import_questions q ON q.id = pa.question_id;",
        "DELETE ar FROM answer_record ar JOIN tmp_chemistry_import_questions q ON q.id = ar.question_id;",
        "DELETE mb FROM mistake_book mb JOIN tmp_chemistry_import_questions q ON q.id = mb.question_id;",
        "DELETE eq FROM exam_question eq JOIN tmp_chemistry_import_questions q ON q.id = eq.question_id;",
        "DELETE eq FROM exam_question eq JOIN tmp_chemistry_import_exams e ON e.id = eq.exam_id;",
        "DELETE es FROM exam_submission es JOIN tmp_chemistry_import_exams e ON e.id = es.exam_id;",
        "DELETE FROM exam_paper WHERE id IN (SELECT id FROM tmp_chemistry_import_exams);",
        "DELETE FROM question WHERE id IN (SELECT id FROM tmp_chemistry_import_questions);",
        "INSERT INTO subject (name, description, icon, sort_order) "
        "SELECT '化学', '化学科目', 'flask', 5 WHERE NOT EXISTS (SELECT 1 FROM subject WHERE name = '化学');",
        "SET @admin_id := COALESCE((SELECT id FROM user WHERE role = 'ADMIN' ORDER BY id LIMIT 1), (SELECT MIN(id) FROM user));",
        "SET @subject_chemistry := (SELECT id FROM subject WHERE name = '化学' ORDER BY id LIMIT 1);",
    ]

    for name, description in config["knowledge"]:
        var_name = common.sql_var_name(config["subject"], name)
        lines.append(
            "INSERT INTO knowledge_point (subject_id, name, parent_id, level, description) "
            f"SELECT @subject_chemistry, {common.sql_str(name)}, 0, 1, {common.sql_str(description)} "
            f"WHERE NOT EXISTS (SELECT 1 FROM knowledge_point WHERE subject_id = @subject_chemistry AND name = {common.sql_str(name)});"
        )
        lines.append(
            f"SET {var_name} := (SELECT id FROM knowledge_point WHERE subject_id = @subject_chemistry "
            f"AND name = {common.sql_str(name)} ORDER BY id LIMIT 1);"
        )

    lines.extend(
        [
            "INSERT INTO exam_paper (name, subject_id, creator_id, total_score, duration, description, status, created_at) "
            "VALUES ('2025全国卷理综化学真题训练', @subject_chemistry, @admin_id, 100, 90, "
            "'2025全国卷理综化学导入真题，含题图、答案和解析', 1, NOW());",
            "SET @exam_id := LAST_INSERT_ID();",
        ]
    )

    imported = 0
    type_counts: dict[str, int] = {}
    for number in sorted(numbers):
        block = blocks.get(number)
        if not block:
            continue

        answer_detail = answers.get(number, {"answer": "", "analysis": ""})
        qtype = common.question_type("chemistry", number, answer_detail["answer"])
        stem, options = common.parse_options(block) if qtype != "ESSAY" else (block, [])
        source_label = f"【2025全国卷理综化学·第{number}题】"
        content_html = common.to_html(f"{source_label}\n{stem}")
        assigned = [
            image_urls[i - 1]
            for i in config.get("image_map", {}).get(number, [])
            if 0 < i <= len(image_urls)
        ]
        content_html = common.append_images(content_html, assigned)
        knowledge_name = common.tag_knowledge("chemistry", number, block)
        knowledge_var = common.sql_var_name(config["subject"], knowledge_name)
        answer_html = common.to_html(answer_detail["answer"])
        analysis_html = common.to_html(answer_detail["analysis"])
        score = 6 if qtype != "ESSAY" else 15

        lines.append(
            "INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by, created_at) "
            f"VALUES (@subject_chemistry, CAST({knowledge_var} AS CHAR), {common.sql_str(qtype)}, {common.sql_str(content_html)}, "
            f"{common.sql_str(common.option_json(options))}, {common.sql_str(answer_html)}, {common.sql_str(analysis_html)}, "
            f"{common.difficulty('chemistry', number, qtype)}, '2025全国卷理综化学', @admin_id, NOW());"
        )
        lines.append("SET @question_id := LAST_INSERT_ID();")
        lines.append(
            f"INSERT INTO exam_question (exam_id, question_id, score, sort_order) VALUES (@exam_id, @question_id, {score}, {number});"
        )
        imported += 1
        type_counts[qtype] = type_counts.get(qtype, 0) + 1

    lines.extend(
        [
            "DROP TEMPORARY TABLE tmp_chemistry_import_questions;",
            "DROP TEMPORARY TABLE tmp_chemistry_import_exams;",
            "COMMIT;",
        ]
    )
    return "\n".join(lines) + "\n", imported, type_counts


def main() -> None:
    sql, count, type_counts = build_chemistry_import()
    SQL_PATH.write_text(sql, encoding="utf-8")
    print(f"wrote_sql={SQL_PATH}")
    print(f"chemistry_questions={count}")
    for qtype, type_count in sorted(type_counts.items()):
        print(f"{qtype}={type_count}")


if __name__ == "__main__":
    main()
