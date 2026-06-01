from __future__ import annotations

import hashlib
import html
import json
import re
import struct
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageFile


END = 0xFFFFFFFE
FREE = 0xFFFFFFFF

WORKSPACE = Path(__file__).resolve().parents[1]
STATIC_ROOT = WORKSPACE / "src" / "main" / "resources" / "static"
IMAGE_ROOT = STATIC_ROOT / "images" / "questions" / "2025"
SQL_PATH = WORKSPACE / "src" / "main" / "resources" / "db_migration_import_science_2025.sql"

ImageFile.LOAD_TRUNCATED_IMAGES = True


DOCS = {
    "chemistry": {
        "path": WORKSPACE / "chem_2025.doc",
        "subject": "化学",
        "source": "2025全国卷理综化学",
        "exam": "2025全国卷理综化学真题训练",
        "choice_numbers": set(range(7, 14)),
        "essay_numbers": set(range(26, 30)),
        "split_marker": "2025年全国卷化学官方参考答案",
        "question_section": "一、选择题",
        "image_map": {
            9: [1],
            10: [2],
            11: [3],
            12: [4],
            13: [5],
            26: [6],
            27: [7, 8],
            28: [9],
            29: [10, 11, 12, 13, 14, 15, 16, 17, 18, 19],
        },
        "knowledge": [
            ("化学与生活", "材料、食品、环境和生活中的化学应用"),
            ("电化学", "原电池、电解池、金属腐蚀与防护"),
            ("有机化学", "有机物结构、性质、合成路线与反应类型"),
            ("物质结构与元素周期律", "原子结构、元素周期律、化学键与分子结构"),
            ("化学反应原理", "反应速率、化学平衡、热化学与溶液平衡"),
            ("化学实验", "实验方案、现象分析、仪器与操作"),
            ("化学计算", "物质的量、平衡常数和综合定量分析"),
        ],
    },
    "biology": {
        "path": WORKSPACE / "bio_2025.doc",
        "subject": "生物",
        "source": "2025高考课标卷生物",
        "exam": "2025高考课标卷生物真题训练",
        "choice_numbers": set(range(1, 7)),
        "essay_numbers": set(range(7, 12)),
        "split_marker": "【1题答案】",
        "question_section": "一、选择题",
        "image_map": {
            2: [1],
            6: [3],
            8: [4],
            11: [5],
        },
        "knowledge": [
            ("生命的物质基础", "蛋白质、核酸等生命大分子的结构与功能"),
            ("细胞代谢", "光合作用、呼吸作用和物质能量变化"),
            ("生命活动调节", "神经调节、体液调节和激素调节"),
            ("遗传与育种", "遗传规律、变异、育种和性状分析"),
            ("生态系统", "种群、群落、生态系统结构与稳定性"),
            ("免疫调节", "免疫系统、免疫失调和免疫应答"),
            ("生物技术与实验", "PCR、电泳、基因工程和实验设计"),
            ("细胞结构与物质运输", "细胞结构、渗透作用和跨膜运输"),
        ],
    },
    "physics": {
        "path": WORKSPACE / "physics_2025.doc",
        "subject": "物理",
        "source": "2025高考课标卷物理",
        "exam": "2025高考课标卷物理真题训练",
        "choice_numbers": set(range(1, 9)),
        "essay_numbers": set(range(9, 13)),
        "split_marker": "【1题答案】",
        "question_section": "二、选择题",
        "image_map": {
            3: [1],
            5: [2],
            6: [3],
            7: [4],
            8: [5],
            9: [6, 7],
            10: [8, 9],
            11: [10],
            12: [11],
        },
        "knowledge": [
            ("运动学与动力学", "直线运动、牛顿运动定律和受力分析"),
            ("万有引力与航天", "天体运动、卫星轨道和引力规律"),
            ("功和能", "动能定理、机械能守恒和能量转化"),
            ("电场", "电势、电场强度和带电粒子运动"),
            ("磁场", "洛伦兹力、带电粒子在磁场中的运动"),
            ("热学", "理想气体、状态参量和热力学图像"),
            ("波动与振动", "机械波、振动和图像分析"),
            ("电学实验", "电路测量、传感器和实验数据处理"),
            ("电容器", "电容器、电介质和电场能量"),
            ("动量与机械能", "碰撞、弹簧模型和综合力学"),
        ],
    },
}


def u16(data: bytes, offset: int) -> int:
    return struct.unpack_from("<H", data, offset)[0]


def u32(data: bytes, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


def u64(data: bytes, offset: int) -> int:
    return struct.unpack_from("<Q", data, offset)[0]


class OleDoc:
    def __init__(self, path: Path):
        self.path = path
        self.data = path.read_bytes()
        header = self.data[:512]
        if header[:8] != bytes.fromhex("D0CF11E0A1B11AE1"):
            raise ValueError(f"{path} is not an old Word .doc file")

        self.sector_size = 1 << u16(header, 30)
        self.mini_sector_size = 1 << u16(header, 32)
        self.num_fat = u32(header, 44)
        self.first_dir = u32(header, 48)
        self.mini_cutoff = u32(header, 56)
        self.first_minifat = u32(header, 60)
        self.num_minifat = u32(header, 64)

        difat = [u32(header, 76 + 4 * i) for i in range(109)]
        difat = [x for x in difat if x not in (FREE, END)]
        self.fat: list[int] = []
        for sector in difat[: self.num_fat]:
            raw = self._sector(sector)
            self.fat.extend(u32(raw, 4 * i) for i in range(len(raw) // 4))

        self.entries = self._read_directory()
        root = next((e for e in self.entries if e["type"] == 5), None)
        self.mini_stream = b"" if root is None else self._read_chain(root["start"])[: root["size"]]
        self.minifat: list[int] = []
        if self.first_minifat not in (FREE, END):
            raw = self._read_chain(self.first_minifat)[: self.num_minifat * self.sector_size]
            self.minifat = [u32(raw, 4 * i) for i in range(len(raw) // 4)]

    def _sector(self, sector: int) -> bytes:
        offset = (sector + 1) * self.sector_size
        return self.data[offset : offset + self.sector_size]

    def _read_chain(self, start: int) -> bytes:
        chunks: list[bytes] = []
        seen: set[int] = set()
        sector = start
        while sector not in (END, FREE) and sector not in seen and sector < len(self.fat):
            seen.add(sector)
            chunks.append(self._sector(sector))
            sector = self.fat[sector]
        return b"".join(chunks)

    def _read_mini_chain(self, start: int) -> bytes:
        chunks: list[bytes] = []
        seen: set[int] = set()
        sector = start
        while sector not in (END, FREE) and sector not in seen and sector < len(self.minifat):
            seen.add(sector)
            offset = sector * self.mini_sector_size
            chunks.append(self.mini_stream[offset : offset + self.mini_sector_size])
            sector = self.minifat[sector]
        return b"".join(chunks)

    def _read_directory(self) -> list[dict]:
        raw = self._read_chain(self.first_dir)
        entries: list[dict] = []
        for offset in range(0, len(raw), 128):
            entry = raw[offset : offset + 128]
            if len(entry) < 128:
                break
            name_len = u16(entry, 64)
            name = entry[: name_len - 2].decode("utf-16le", "ignore") if name_len >= 2 else ""
            entries.append(
                {
                    "name": name,
                    "type": entry[66],
                    "start": u32(entry, 116),
                    "size": u64(entry, 120),
                }
            )
        return entries

    def stream(self, name: str) -> bytes:
        entry = next(e for e in self.entries if e["name"] == name)
        if entry["size"] < self.mini_cutoff and entry["type"] == 2:
            return self._read_mini_chain(entry["start"])[: entry["size"]]
        return self._read_chain(entry["start"])[: entry["size"]]


def clean_text(text: str) -> str:
    text = text.replace("\r", "\n").replace("\x07", "\n")
    text = "".join(ch if ch in "\n\t" or ord(ch) >= 32 else " " for ch in text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def extract_doc_text(path: Path) -> tuple[str, OleDoc]:
    ole = OleDoc(path)
    word = ole.stream("WordDocument")
    flags = u16(word, 0x0A)
    table = ole.stream("1Table" if flags & 0x0200 else "0Table")
    fc_clx = u32(word, 0x01A2)
    lcb_clx = u32(word, 0x01A6)
    clx = table[fc_clx : fc_clx + lcb_clx]

    pos = 0
    plc = None
    while pos < len(clx):
        tag = clx[pos]
        pos += 1
        if tag == 1:
            size = u16(clx, pos)
            pos += 2 + size
        elif tag == 2:
            size = u32(clx, pos)
            pos += 4
            plc = clx[pos : pos + size]
            break
        else:
            nxt = clx.find(b"\x02", pos)
            pos = nxt if nxt >= 0 else len(clx)
    if plc is None:
        raise RuntimeError(f"No Word piece table found in {path}")

    piece_count = (len(plc) - 4) // 12
    cps = [u32(plc, 4 * i) for i in range(piece_count + 1)]
    pcd_base = 4 * (piece_count + 1)
    parts: list[str] = []
    for i in range(piece_count):
        pcd = plc[pcd_base + i * 8 : pcd_base + (i + 1) * 8]
        raw_fc = u32(pcd, 2)
        compressed = bool(raw_fc & 0x40000000)
        fc = raw_fc & 0x3FFFFFFF
        cp_len = cps[i + 1] - cps[i]
        if compressed:
            raw = word[fc // 2 : fc // 2 + cp_len]
            try:
                part = raw.decode("gbk")
            except UnicodeDecodeError:
                part = raw.decode("cp1252", "ignore")
        else:
            raw = word[fc : fc + cp_len * 2]
            part = raw.decode("utf-16le", "ignore")
        parts.append(part)
    return clean_text("".join(parts)), ole


def extract_pngs(ole: OleDoc, subject_key: str) -> list[str]:
    data = ole.data
    png_sig = b"\x89PNG\r\n\x1a\n"
    iend = b"IEND\xaeB`\x82"
    out_dir = IMAGE_ROOT / subject_key
    out_dir.mkdir(parents=True, exist_ok=True)
    for old_file in out_dir.glob("img_*.png"):
        old_file.unlink()

    seen: set[str] = set()
    urls: list[str] = []
    cursor = 0
    index = 1
    while True:
        start = data.find(png_sig, cursor)
        if start < 0:
            break
        end = data.find(iend, start)
        if end < 0:
            break
        end += len(iend)
        blob = data[start:end]
        cursor = end
        if len(blob) < 900:
            continue
        try:
            image = Image.open(BytesIO(blob))
            width, height = image.size
            image.load()
        except Exception:
            continue
        if (width, height) == (162, 54):
            continue
        digest = hashlib.sha1(blob).hexdigest()
        if digest in seen:
            continue
        seen.add(digest)
        filename = f"img_{index:03d}.png"
        image.convert("RGBA" if "A" in image.mode else "RGB").save(out_dir / filename, format="PNG")
        urls.append(f"/images/questions/2025/{subject_key}/{filename}")
        index += 1
    return urls


def split_body_and_answers(text: str, marker: str) -> tuple[str, str]:
    idx = text.find(marker)
    if idx < 0:
        body = text
        answers = ""
    else:
        body = text[:idx]
        answers = text[idx:]
    title = "2025年普通高等学校招生全国统一考试"
    first_title = body.find(title)
    second_title = body.find(title, first_title + len(title)) if first_title >= 0 else -1
    if second_title >= 0:
        body = body[:second_title]
    return body, answers


def split_questions(body: str, numbers: set[int]) -> dict[int, str]:
    matches = [
        m
        for m in re.finditer(r"(?m)^\s*(\d{1,2})[．.]\s*", body)
        if int(m.group(1)) in numbers
    ]
    questions: dict[int, str] = {}
    for idx, match in enumerate(matches):
        number = int(match.group(1))
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(body)
        block = body[match.start() : end].strip()
        questions[number] = cleanup_question_text(block)
    return questions


def cleanup_question_text(text: str) -> str:
    text = re.sub(r"第\s*PAGE\s+\d+\s*页/共\s*NUMPAGES\s+\d+\s*页", "", text)
    text = text.replace("学科网（北京）股份有限公司", "")
    text = text.replace("EMBED Equation.DSMT4", "（公式见原题）")
    text = re.sub(r"\n\s*[一二三四]、[^\n]*$", "", text)
    text = re.sub(r"\n\s*绝密★启用前.*$", "", text, flags=re.S)
    text = re.sub(r"\n\s*2025年普通高等学校招生全国统一考试.*$", "", text, flags=re.S)
    text = re.sub(r"\s*\n\s*", "\n", text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


def parse_options(block: str) -> tuple[str, list[str]]:
    matches = list(re.finditer(r"(?m)^[ \t]*([A-D])(?:[.．]|[ \t]+)", block))
    if len(matches) < 4:
        matches = list(re.finditer(r"(?<![A-Za-z0-9])([A-D])[.．]\s*", block))
    if len(matches) < 2:
        return block.strip(), []
    stem = block[: matches[0].start()].strip()
    options: list[str] = []
    for idx, match in enumerate(matches):
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(block)
        options.append(block[match.end() : end].strip())
    return stem, options


def parse_answers(answer_text: str, subject_key: str) -> dict[int, str]:
    answers: dict[int, str] = {}
    if subject_key == "chemistry":
        for match in re.finditer(r"(\d{1,2})[．.]\s*([A-D]+)", answer_text):
            answers[int(match.group(1))] = match.group(2)
        for number in (26, 27, 28, 29):
            pattern = rf"{number}[．.]\s*(.*?)(?=(?:2[6-9])[．.]|第\s*PAGE|学科网|$)"
            match = re.search(pattern, answer_text, flags=re.S)
            if match:
                answers[number] = cleanup_question_text(match.group(1))
        return answers

    for match in re.finditer(r"【(\d{1,2})题答案】\s*【答案】(.*?)(?=【\d{1,2}题答案】|第\s*PAGE|学科网|$)", answer_text, flags=re.S):
        answers[int(match.group(1))] = cleanup_question_text(match.group(2))
    return answers


def option_json(options: list[str]) -> str | None:
    if not options:
        return None
    return json.dumps([to_html(opt) for opt in options], ensure_ascii=False)


def to_html(text: str) -> str:
    text = cleanup_question_text(text)
    escaped = html.escape(text)
    escaped = escaped.replace("\n", "<br>")
    return escaped


def append_images(content_html: str, image_urls: list[str]) -> str:
    if not image_urls:
        return content_html
    imgs = "".join(f'<img src="{html.escape(url)}" alt="题目配图">' for url in image_urls)
    return f'{content_html}<div class="question-images">{imgs}</div>'


def image_need(block: str, qtype: str) -> int:
    triggers = ("如图", "图所示", "如下图", "下图", "曲线", "路线", "装置", "电解池", "结构如图", "电泳结果")
    if not any(t in block for t in triggers):
        return 0
    count = 1
    if qtype == "ESSAY" and ("路线" in block or block.count("图") >= 2):
        count += 1
    return count


def tag_knowledge(subject_key: str, number: int, block: str) -> str:
    if subject_key == "chemistry":
        if number == 7:
            return "化学与生活"
        if number in (8, 10):
            return "电化学"
        if number in (9, 29):
            return "有机化学"
        if number == 11:
            return "物质结构与元素周期律"
        if number in (12, 28):
            return "化学反应原理"
        if number == 26:
            return "化学实验"
        if number == 27:
            return "化学计算"
        return "化学反应原理"
    if subject_key == "biology":
        mapping = {
            1: "生命的物质基础",
            2: "细胞代谢",
            3: "生命活动调节",
            4: "生态系统",
            5: "遗传与育种",
            6: "生物技术与实验",
            7: "细胞结构与物质运输",
            8: "免疫调节",
            9: "生态系统",
            10: "遗传与育种",
            11: "生物技术与实验",
        }
        return mapping.get(number, "生物技术与实验")
    mapping = {
        1: "运动学与动力学",
        2: "万有引力与航天",
        3: "功和能",
        4: "电场",
        5: "磁场",
        6: "热学",
        7: "波动与振动",
        8: "波动与振动",
        9: "电学实验",
        10: "电学实验",
        11: "电容器",
        12: "动量与机械能",
    }
    return mapping.get(number, "运动学与动力学")


def sql_str(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_var_name(subject: str, knowledge: str) -> str:
    raw = f"{subject}_{knowledge}"
    digest = hashlib.md5(raw.encode("utf-8")).hexdigest()[:10]
    return f"@kp_{digest}"


def question_type(subject_key: str, number: int, answer: str | None) -> str:
    if number not in DOCS[subject_key]["choice_numbers"]:
        return "ESSAY"
    if answer and len(re.sub(r"[^A-D]", "", answer)) > 1:
        return "MULTI_CHOICE"
    return "SINGLE_CHOICE"


def difficulty(subject_key: str, number: int, qtype: str) -> int:
    if qtype == "ESSAY":
        return 4 if number >= 10 else 3
    if subject_key == "chemistry" and number >= 11:
        return 4
    if subject_key == "physics" and number >= 6:
        return 4
    return 3


def build_import() -> tuple[str, dict[str, int]]:
    all_questions: list[dict] = []
    stats: dict[str, int] = {}

    for subject_key, config in DOCS.items():
        text, ole = extract_doc_text(config["path"])
        body, answer_text = split_body_and_answers(text, config["split_marker"])
        section_pos = body.find(config["question_section"])
        if section_pos >= 0:
            body = body[section_pos:]
        numbers = set(config["choice_numbers"]) | set(config["essay_numbers"])
        blocks = split_questions(body, numbers)
        answers = parse_answers(answer_text, subject_key)
        image_urls = extract_pngs(ole, subject_key)

        for number in sorted(numbers):
            block = blocks.get(number)
            if not block:
                continue
            answer = answers.get(number, "")
            qtype = question_type(subject_key, number, answer)
            stem, options = parse_options(block) if qtype != "ESSAY" else (block, [])
            source_label = f"【{config['source']}·第{number}题】"
            content_html = to_html(f"{source_label}\n{stem}")
            assigned = [
                image_urls[i - 1]
                for i in config.get("image_map", {}).get(number, [])
                if 0 < i <= len(image_urls)
            ]
            if not assigned and subject_key not in ("chemistry", "biology", "physics"):
                need = image_need(block, qtype)
                assigned = image_urls[:need]
            content_html = append_images(content_html, assigned)
            knowledge_name = tag_knowledge(subject_key, number, block)
            answer_html = to_html(answer)
            analysis_html = answer_html if qtype == "ESSAY" else ""
            all_questions.append(
                {
                    "subject_key": subject_key,
                    "subject": config["subject"],
                    "source": config["source"],
                    "exam": config["exam"],
                    "number": number,
                    "type": qtype,
                    "content": content_html,
                    "options": option_json(options),
                    "answer": answer_html,
                    "analysis": analysis_html,
                    "difficulty": difficulty(subject_key, number, qtype),
                    "knowledge": knowledge_name,
                    "score": 6 if qtype != "ESSAY" else 15,
                }
            )
        stats[subject_key] = len([q for q in all_questions if q["subject_key"] == subject_key])

    lines: list[str] = []
    lines.append("-- Import 2025 science subject exam questions with images and answers.")
    lines.append("USE smart_learning;")
    lines.append("START TRANSACTION;")
    sources = ", ".join(sql_str(config["source"]) for config in DOCS.values())
    exams = ", ".join(sql_str(config["exam"]) for config in DOCS.values())
    lines.extend(
        [
            "CREATE TEMPORARY TABLE tmp_science_import_questions AS",
            f"SELECT id FROM question WHERE source IN ({sources});",
            "DELETE pa FROM practice_answer pa JOIN tmp_science_import_questions tq ON tq.id = pa.question_id;",
            "DELETE ar FROM answer_record ar JOIN tmp_science_import_questions tq ON tq.id = ar.question_id;",
            "DELETE mb FROM mistake_book mb JOIN tmp_science_import_questions tq ON tq.id = mb.question_id;",
            "DELETE eq FROM exam_question eq JOIN tmp_science_import_questions tq ON tq.id = eq.question_id;",
            "DELETE q FROM question q JOIN tmp_science_import_questions tq ON tq.id = q.id;",
            "DROP TEMPORARY TABLE tmp_science_import_questions;",
            f"DELETE eq FROM exam_question eq JOIN exam_paper ep ON ep.id = eq.exam_id WHERE ep.name IN ({exams});",
            f"DELETE FROM exam_paper WHERE name IN ({exams});",
            "SET @admin_id := COALESCE((SELECT id FROM user WHERE role = 'ADMIN' ORDER BY id LIMIT 1), (SELECT MIN(id) FROM user));",
        ]
    )

    for subject_key, config in DOCS.items():
        subject = config["subject"]
        subject_var = f"@subject_{subject_key}"
        lines.append(
            "INSERT INTO subject (name, description, icon, sort_order) "
            f"SELECT {sql_str(subject)}, {sql_str(subject + '学科')}, {sql_str(subject_icon(subject))}, {subject_order(subject)} "
            f"WHERE NOT EXISTS (SELECT 1 FROM subject WHERE name = {sql_str(subject)});"
        )
        lines.append(f"SET {subject_var} := (SELECT id FROM subject WHERE name = {sql_str(subject)} ORDER BY id LIMIT 1);")
        for name, description in config["knowledge"]:
            lines.append(
                "INSERT INTO knowledge_point (subject_id, name, parent_id, level, description) "
                f"SELECT {subject_var}, {sql_str(name)}, 0, 1, {sql_str(description)} "
                f"WHERE NOT EXISTS (SELECT 1 FROM knowledge_point WHERE subject_id = {subject_var} AND name = {sql_str(name)});"
            )
            lines.append(
                f"SET {sql_var_name(subject, name)} := "
                f"(SELECT id FROM knowledge_point WHERE subject_id = {subject_var} AND name = {sql_str(name)} ORDER BY id LIMIT 1);"
            )

    current_exam: str | None = None
    for q in all_questions:
        if current_exam != q["exam"]:
            current_exam = q["exam"]
            subject = q["subject"]
            subject_var = f"@subject_{q['subject_key']}"
            lines.append(
                "INSERT INTO exam_paper (name, subject_id, creator_id, total_score, duration, description, status, created_at) "
                f"VALUES ({sql_str(q['exam'])}, {subject_var}, @admin_id, 100, 90, "
                f"{sql_str(q['source'] + '导入真题，含答案解析和题图')}, 1, NOW());"
            )
            lines.append("SET @exam_id := LAST_INSERT_ID();")

        subject = q["subject"]
        subject_var = f"@subject_{q['subject_key']}"
        kp_var = sql_var_name(subject, q["knowledge"])
        lines.append(
            "INSERT INTO question (subject_id, knowledge_ids, type, content, options, answer, analysis, difficulty, source, created_by, created_at) "
            f"VALUES ({subject_var}, CAST({kp_var} AS CHAR), {sql_str(q['type'])}, {sql_str(q['content'])}, "
            f"{sql_str(q['options'])}, {sql_str(q['answer'])}, {sql_str(q['analysis'])}, {q['difficulty']}, {sql_str(q['source'])}, @admin_id, NOW());"
        )
        lines.append("SET @question_id := LAST_INSERT_ID();")
        lines.append(
            "INSERT INTO exam_question (exam_id, question_id, score, sort_order) "
            f"VALUES (@exam_id, @question_id, {q['score']}, {q['number']});"
        )

    lines.append("COMMIT;")
    return "\n".join(lines) + "\n", stats


def subject_icon(subject: str) -> str:
    return {"物理": "atom", "化学": "flask", "生物": "dna"}.get(subject, "book")


def subject_order(subject: str) -> int:
    return {"物理": 4, "化学": 5, "生物": 6}.get(subject, 99)


def main() -> None:
    sql, stats = build_import()
    SQL_PATH.write_text(sql, encoding="utf-8")
    print(f"wrote_sql={SQL_PATH}")
    for key, count in stats.items():
        print(f"{key}_questions={count}")


if __name__ == "__main__":
    main()
