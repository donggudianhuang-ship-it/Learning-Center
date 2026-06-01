/*
 Navicat Premium Data Transfer

 Source Server         : 111
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : smart_learning

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 30/05/2026 18:47:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_grading_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_grading_record`;
CREATE TABLE `ai_grading_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `subject` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `grade` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `total_score` decimal(7, 2) NULL DEFAULT 0.00,
  `max_score` decimal(7, 2) NULL DEFAULT 0.00,
  `accuracy_rate` decimal(7, 2) NULL DEFAULT 0.00,
  `correct_count` int NULL DEFAULT 0,
  `wrong_count` int NULL DEFAULT 0,
  `result_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_grading_record_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `ai_grading_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_grading_record
-- ----------------------------
INSERT INTO `ai_grading_record` VALUES (1, 6, '数学判卷 - 屏幕截图 2026-05-28 104314.png', '数学', '高中', 0.00, 0.00, 0.00, 0, 0, '{\"accuracyRate\":0,\"aiSummary\":\"根据诊断结论，学生当前处于基础补强期，整体正确率偏低（29.4%），主要问题集中在函数与导数，而立体几何为优势。建议先完成今日到期错题复盘，重点加强函数与导数的复习和练习。\",\"correctCount\":0,\"errorPoints\":[],\"grade\":\"高中\",\"maxScore\":0,\"questions\":[],\"subject\":\"数学\",\"totalScore\":0,\"wrongCount\":0}', '屏幕截图 2026-05-28 104314.png', '2026-05-30 11:02:41');
INSERT INTO `ai_grading_record` VALUES (2, 6, '数学判卷 - 屏幕截图 2026-05-27 092740.png', '数学', '高中', 0.00, 0.00, 0.00, 0, 0, '{\"accuracyRate\":0,\"aiSummary\":\"上传内容为一份学习计划安排表，并非具体试卷或作业题目，因此无法进行批改。请上传包含具体题目的试卷/作业内容。\",\"correctCount\":0,\"errorPoints\":[],\"grade\":\"高中\",\"maxScore\":0,\"questions\":[],\"subject\":\"数学\",\"totalScore\":0,\"wrongCount\":0}', '屏幕截图 2026-05-27 092740.png', '2026-05-30 11:02:59');
INSERT INTO `ai_grading_record` VALUES (3, 6, '信息技术判卷 - 屏幕截图 2025-09-17 111618.png', '信息技术', '高中', 10.00, 10.00, 100.00, 1, 0, '{\"accuracyRate\":100.00,\"aiSummary\":\"该生第39题回答正确，掌握了typedef的基本概念和常见错误，建议后续关注代码风格规范。\",\"correctCount\":1,\"errorPoints\":[],\"grade\":\"高中\",\"maxScore\":10,\"questions\":[{\"aiAnalysis\":\"学生选择了正确的选项C。本题考察typedef的语法规则，C选项错误，因为typedef定义的类型名不必使用大写字母，通常为了风格一致会使用大写，但小写也是合法的。其他选项A和D的描述正确。\",\"correctAnswer\":\"C\",\"isCorrect\":true,\"knowledgePoints\":\"typedef关键字用法\",\"maxScore\":10,\"mistakeType\":\"\",\"questionContent\":\"以下叙述中错误的是 ( ) 。A. 可以用typedef说明的新类型名来定义变量 C. typedef说明的新类型名必须使用大写字母，否则会出编译错误 D. 用typedef可以说明一种新的类型名\",\"questionNumber\":1,\"questionType\":\"SINGLE_CHOICE\",\"score\":10,\"suggestion\":\"可以进一步复习typedef的具体用法，注意类型名大小写并不影响编译，但建议保持代码风格一致。\",\"userAnswer\":\"C\"}],\"subject\":\"信息技术\",\"totalScore\":10,\"wrongCount\":0}', '屏幕截图 2025-09-17 111618.png', '2026-05-30 18:16:44');

-- ----------------------------
-- Table structure for answer_record
-- ----------------------------
DROP TABLE IF EXISTS `answer_record`;
CREATE TABLE `answer_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `exam_submission_id` bigint NULL DEFAULT NULL COMMENT 'NULL if practice mode',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_correct` tinyint NULL DEFAULT NULL COMMENT '0-wrong, 1-correct, NULL for subjective',
  `score` decimal(5, 1) NULL DEFAULT 0.0,
  `ai_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI analysis of the answer',
  `mistake_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'CONCEPT_ERROR, CARELESS, WRONG_APPROACH, etc.',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_question`(`question_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  CONSTRAINT `answer_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `answer_record_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 311 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of answer_record
-- ----------------------------
INSERT INTO `answer_record` VALUES (78, 10, 22, NULL, 'A,B', 0, 0.0, NULL, NULL, '2026-05-20 10:03:27');
INSERT INTO `answer_record` VALUES (148, 6, 20, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-26 11:05:17');
INSERT INTO `answer_record` VALUES (149, 6, 49, NULL, 'll', 0, 0.0, NULL, NULL, '2026-05-26 11:05:29');
INSERT INTO `answer_record` VALUES (150, 6, 14, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 11:05:45');
INSERT INTO `answer_record` VALUES (151, 6, 46, NULL, '来了，', 0, 0.0, NULL, NULL, '2026-05-26 11:05:54');
INSERT INTO `answer_record` VALUES (154, 6, 16, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-26 11:06:10');
INSERT INTO `answer_record` VALUES (155, 6, 44, NULL, '；，；l', 0, 0.0, NULL, NULL, '2026-05-26 11:06:17');
INSERT INTO `answer_record` VALUES (156, 6, 25, NULL, '-1', 0, 0.0, NULL, NULL, '2026-05-26 11:06:26');
INSERT INTO `answer_record` VALUES (157, 6, 19, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-26 11:06:31');
INSERT INTO `answer_record` VALUES (158, 6, 23, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-26 11:06:37');
INSERT INTO `answer_record` VALUES (159, 6, 22, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-26 11:06:42');
INSERT INTO `answer_record` VALUES (160, 6, 15, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 11:45:01');
INSERT INTO `answer_record` VALUES (161, 6, 14, NULL, 'B', 1, 10.0, NULL, NULL, '2026-05-26 11:45:08');
INSERT INTO `answer_record` VALUES (162, 6, 18, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-26 11:45:15');
INSERT INTO `answer_record` VALUES (163, 6, 16, NULL, 'A', 1, 10.0, NULL, NULL, '2026-05-26 11:45:19');
INSERT INTO `answer_record` VALUES (164, 6, 12, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 11:45:23');
INSERT INTO `answer_record` VALUES (165, 6, 13, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-26 11:45:27');
INSERT INTO `answer_record` VALUES (166, 6, 17, NULL, 'A', 1, 10.0, NULL, NULL, '2026-05-26 11:45:32');
INSERT INTO `answer_record` VALUES (167, 6, 19, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 11:45:38');
INSERT INTO `answer_record` VALUES (168, 6, 25, NULL, '1', 0, 0.0, NULL, NULL, '2026-05-26 17:04:39');
INSERT INTO `answer_record` VALUES (169, 6, 49, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-26 17:04:41');
INSERT INTO `answer_record` VALUES (170, 6, 44, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-26 17:05:02');
INSERT INTO `answer_record` VALUES (171, 6, 19, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-26 17:05:05');
INSERT INTO `answer_record` VALUES (172, 6, 16, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-26 17:05:07');
INSERT INTO `answer_record` VALUES (173, 6, 22, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 17:05:12');
INSERT INTO `answer_record` VALUES (174, 6, 20, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-26 17:05:16');
INSERT INTO `answer_record` VALUES (175, 6, 23, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-26 17:05:18');
INSERT INTO `answer_record` VALUES (176, 6, 46, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-26 17:05:20');
INSERT INTO `answer_record` VALUES (177, 6, 14, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-26 17:05:25');
INSERT INTO `answer_record` VALUES (178, 6, 12, NULL, 'A', 1, 10.0, NULL, NULL, '2026-05-26 17:05:34');
INSERT INTO `answer_record` VALUES (179, 6, 15, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-26 17:05:37');
INSERT INTO `answer_record` VALUES (180, 6, 19, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 17:05:39');
INSERT INTO `answer_record` VALUES (181, 6, 18, NULL, 'A', 1, 10.0, NULL, NULL, '2026-05-26 17:05:42');
INSERT INTO `answer_record` VALUES (182, 6, 13, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 17:05:45');
INSERT INTO `answer_record` VALUES (183, 6, 19, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-26 17:06:02');
INSERT INTO `answer_record` VALUES (184, 6, 15, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-26 17:06:05');
INSERT INTO `answer_record` VALUES (185, 6, 13, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-26 17:06:08');
INSERT INTO `answer_record` VALUES (186, 6, 13, NULL, 'A', 1, 10.0, NULL, NULL, '2026-05-26 17:06:14');
INSERT INTO `answer_record` VALUES (187, 6, 15, NULL, 'B', 1, 10.0, NULL, NULL, '2026-05-26 17:06:17');
INSERT INTO `answer_record` VALUES (188, 6, 19, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-26 17:06:20');
INSERT INTO `answer_record` VALUES (189, 6, 19, NULL, 'B', 1, 10.0, NULL, NULL, '2026-05-26 17:06:32');
INSERT INTO `answer_record` VALUES (225, 6, 92, NULL, 'erthj', 0, 0.0, NULL, NULL, '2026-05-27 08:54:28');
INSERT INTO `answer_record` VALUES (226, 6, 91, NULL, '玩y', 0, 0.0, NULL, NULL, '2026-05-27 08:54:32');
INSERT INTO `answer_record` VALUES (227, 6, 45, NULL, 'qawer', 0, 0.0, NULL, NULL, '2026-05-27 08:54:36');
INSERT INTO `answer_record` VALUES (228, 6, 94, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:54:40');
INSERT INTO `answer_record` VALUES (229, 6, 47, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:54:55');
INSERT INTO `answer_record` VALUES (230, 6, 48, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:54:57');
INSERT INTO `answer_record` VALUES (231, 6, 93, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:55:00');
INSERT INTO `answer_record` VALUES (232, 6, 90, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:55:02');
INSERT INTO `answer_record` VALUES (233, 6, 84, NULL, 'A', 1, 10.0, NULL, NULL, '2026-05-27 08:57:04');
INSERT INTO `answer_record` VALUES (234, 6, 86, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-27 08:57:07');
INSERT INTO `answer_record` VALUES (235, 6, 88, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-27 08:57:10');
INSERT INTO `answer_record` VALUES (236, 6, 89, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-27 08:57:13');
INSERT INTO `answer_record` VALUES (237, 6, 85, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-27 08:57:16');
INSERT INTO `answer_record` VALUES (238, 6, 87, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-27 08:57:19');
INSERT INTO `answer_record` VALUES (239, 6, 91, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:57:35');
INSERT INTO `answer_record` VALUES (240, 6, 45, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:57:42');
INSERT INTO `answer_record` VALUES (241, 6, 92, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 08:57:45');
INSERT INTO `answer_record` VALUES (242, 6, 90, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 09:18:34');
INSERT INTO `answer_record` VALUES (243, 6, 91, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 09:18:36');
INSERT INTO `answer_record` VALUES (244, 6, 94, NULL, '2', 1, 10.0, NULL, NULL, '2026-05-27 09:18:40');
INSERT INTO `answer_record` VALUES (245, 6, 86, NULL, 'C', 1, 10.0, NULL, NULL, '2026-05-27 09:18:58');
INSERT INTO `answer_record` VALUES (246, 6, 87, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-27 09:19:02');
INSERT INTO `answer_record` VALUES (247, 6, 88, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-27 09:19:05');
INSERT INTO `answer_record` VALUES (248, 6, 89, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-27 09:19:07');
INSERT INTO `answer_record` VALUES (249, 6, 85, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-27 09:19:09');
INSERT INTO `answer_record` VALUES (250, 6, 84, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-27 09:19:12');
INSERT INTO `answer_record` VALUES (251, 6, 86, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-27 09:19:15');
INSERT INTO `answer_record` VALUES (252, 6, 15, NULL, 'B', 1, 10.0, NULL, NULL, '2026-05-27 09:19:24');
INSERT INTO `answer_record` VALUES (253, 6, 13, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-27 09:19:28');
INSERT INTO `answer_record` VALUES (254, 6, 16, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-27 09:19:30');
INSERT INTO `answer_record` VALUES (255, 6, 14, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-27 09:19:32');
INSERT INTO `answer_record` VALUES (256, 6, 18, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-27 09:19:34');
INSERT INTO `answer_record` VALUES (257, 6, 19, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-27 09:19:37');
INSERT INTO `answer_record` VALUES (258, 6, 17, NULL, 'D', 0, 0.0, NULL, NULL, '2026-05-27 09:19:38');
INSERT INTO `answer_record` VALUES (259, 6, 49, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 09:20:05');
INSERT INTO `answer_record` VALUES (260, 6, 23, NULL, 'A', 0, 0.0, NULL, NULL, '2026-05-27 09:20:07');
INSERT INTO `answer_record` VALUES (261, 6, 22, NULL, 'CD', 0, 0.0, NULL, NULL, '2026-05-27 09:20:10');
INSERT INTO `answer_record` VALUES (262, 6, 20, NULL, 'B', 0, 0.0, NULL, NULL, '2026-05-27 09:20:12');
INSERT INTO `answer_record` VALUES (263, 6, 16, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-27 09:20:14');
INSERT INTO `answer_record` VALUES (264, 6, 44, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 09:20:16');
INSERT INTO `answer_record` VALUES (265, 6, 25, NULL, '1', 0, 0.0, NULL, NULL, '2026-05-27 09:20:18');
INSERT INTO `answer_record` VALUES (266, 6, 14, NULL, 'B', 1, 10.0, NULL, NULL, '2026-05-27 09:20:19');
INSERT INTO `answer_record` VALUES (267, 6, 46, NULL, '2', 1, 10.0, NULL, NULL, '2026-05-27 09:20:21');
INSERT INTO `answer_record` VALUES (268, 6, 19, NULL, 'C', 0, 0.0, NULL, NULL, '2026-05-27 09:20:23');
INSERT INTO `answer_record` VALUES (269, 6, 25, NULL, '1', 0, 0.0, NULL, NULL, '2026-05-27 11:27:24');
INSERT INTO `answer_record` VALUES (270, 6, 46, NULL, '1', 1, 10.0, NULL, NULL, '2026-05-27 11:27:50');
INSERT INTO `answer_record` VALUES (271, 6, 44, NULL, '2', 1, 10.0, NULL, NULL, '2026-05-27 11:27:53');
INSERT INTO `answer_record` VALUES (272, 6, 49, NULL, '3', 1, 10.0, NULL, NULL, '2026-05-27 11:27:55');
INSERT INTO `answer_record` VALUES (273, 6, 46, NULL, '0', 0, 0.0, NULL, NULL, '2026-05-27 11:31:17');
INSERT INTO `answer_record` VALUES (274, 6, 45, NULL, '3', 0, 0.0, NULL, NULL, '2026-05-27 11:31:25');
INSERT INTO `answer_record` VALUES (275, 6, 47, NULL, '2', 1, 10.0, NULL, NULL, '2026-05-27 11:31:29');
INSERT INTO `answer_record` VALUES (276, 6, 47, NULL, '12', 0, 0.0, NULL, NULL, '2026-05-27 11:31:41');
INSERT INTO `answer_record` VALUES (277, 6, 45, NULL, '2', 1, 10.0, NULL, NULL, '2026-05-27 11:31:45');
INSERT INTO `answer_record` VALUES (278, 6, 44, NULL, '2', 1, 10.0, NULL, NULL, '2026-05-27 11:31:47');
INSERT INTO `answer_record` VALUES (279, 6, 91, NULL, '1', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:18:16');
INSERT INTO `answer_record` VALUES (280, 6, 90, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:18:29');
INSERT INTO `answer_record` VALUES (281, 6, 93, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:18:32');
INSERT INTO `answer_record` VALUES (282, 6, 92, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:18:40');
INSERT INTO `answer_record` VALUES (283, 6, 90, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:18:46');
INSERT INTO `answer_record` VALUES (284, 6, 94, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:18:51');
INSERT INTO `answer_record` VALUES (285, 6, 91, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 1, 10.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', NULL, '2026-05-27 14:19:00');
INSERT INTO `answer_record` VALUES (286, 6, 92, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:19:28');
INSERT INTO `answer_record` VALUES (287, 6, 94, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:19:36');
INSERT INTO `answer_record` VALUES (288, 6, 92, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:22:34');
INSERT INTO `answer_record` VALUES (289, 6, 93, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:22:36');
INSERT INTO `answer_record` VALUES (290, 6, 94, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:22:40');
INSERT INTO `answer_record` VALUES (291, 6, 94, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:22:46');
INSERT INTO `answer_record` VALUES (292, 6, 92, NULL, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:22:51');
INSERT INTO `answer_record` VALUES (293, 6, 91, NULL, '（1）自身免疫病、免疫缺陷病\n（2）抑制 、 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量的改变\n（3）酶E 、再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.0, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', 'INCOMPLETE', '2026-05-27 14:24:14');
INSERT INTO `answer_record` VALUES (294, 6, 19, NULL, 'D', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 08:56:11');
INSERT INTO `answer_record` VALUES (295, 6, 16, NULL, 'D', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 08:56:14');
INSERT INTO `answer_record` VALUES (296, 6, 13, NULL, 'D', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 08:56:16');
INSERT INTO `answer_record` VALUES (297, 6, 108, NULL, 'B', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:28:09');
INSERT INTO `answer_record` VALUES (298, 6, 109, NULL, 'C', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:28:12');
INSERT INTO `answer_record` VALUES (299, 6, 106, NULL, 'A', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:28:14');
INSERT INTO `answer_record` VALUES (300, 6, 111, NULL, 'C', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:28:16');
INSERT INTO `answer_record` VALUES (301, 6, 107, NULL, 'D', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:28:18');
INSERT INTO `answer_record` VALUES (302, 6, 112, NULL, 'A', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:28:21');
INSERT INTO `answer_record` VALUES (303, 6, 110, NULL, 'C', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:28:24');
INSERT INTO `answer_record` VALUES (304, 6, 120, NULL, 'D', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:56:00');
INSERT INTO `answer_record` VALUES (305, 6, 118, NULL, 'C', 1, 10.0, '回答正确。', NULL, '2026-05-28 09:56:02');
INSERT INTO `answer_record` VALUES (306, 6, 121, NULL, 'B', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:56:05');
INSERT INTO `answer_record` VALUES (307, 6, 119, NULL, 'A', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:56:08');
INSERT INTO `answer_record` VALUES (308, 6, 117, NULL, 'C', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:56:10');
INSERT INTO `answer_record` VALUES (309, 6, 120, NULL, 'A', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:56:13');
INSERT INTO `answer_record` VALUES (310, 6, 118, NULL, 'D', 0, 0.0, '答案错误，请结合解析订正。', 'CONCEPT_ERROR', '2026-05-28 09:56:15');

-- ----------------------------
-- Table structure for collect_record
-- ----------------------------
DROP TABLE IF EXISTS `collect_record`;
CREATE TABLE `collect_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_type` enum('POST','NOTE','COURSE','QUESTION') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_target`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  CONSTRAINT `collect_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of collect_record
-- ----------------------------

-- ----------------------------
-- Table structure for community_post
-- ----------------------------
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `subject_id` bigint NULL DEFAULT NULL,
  `anonymous` tinyint NULL DEFAULT 0,
  `view_count` int NULL DEFAULT 0,
  `like_count` int NULL DEFAULT 0,
  `comment_count` int NULL DEFAULT 0,
  `status` tinyint NULL DEFAULT 1 COMMENT '0-hidden, 1-visible',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_subject`(`subject_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  CONSTRAINT `community_post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `community_post_ibfk_2` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of community_post
-- ----------------------------
INSERT INTO `community_post` VALUES (1, 2, '不是吗', '是的', NULL, 0, 13, 3, 4, 1, '2026-03-30 14:36:42', '2026-03-30 14:36:42');
INSERT INTO `community_post` VALUES (2, 2, 'sb', 'cnm\n', NULL, 1, 0, 0, 0, 1, '2026-03-30 15:01:03', '2026-03-30 15:01:03');
INSERT INTO `community_post` VALUES (3, 4, 'q\'we\'r', 'q\'we\'r', NULL, 1, 1, 0, 1, 1, '2026-03-30 16:33:11', '2026-03-30 16:33:11');
INSERT INTO `community_post` VALUES (4, 6, '额外全额委屈恶气', '恶趣味我去饿我去', NULL, 1, 1, 0, 0, 1, '2026-05-28 18:01:48', '2026-05-28 18:01:48');

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `subject_id` bigint NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `video_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `duration` int NULL DEFAULT NULL COMMENT 'seconds',
  `teacher_id` bigint NULL DEFAULT NULL,
  `teacher_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `view_count` int NULL DEFAULT 0,
  `like_count` int NULL DEFAULT 0,
  `difficulty` tinyint NULL DEFAULT 3,
  `status` tinyint NULL DEFAULT 1 COMMENT '0-draft, 1-published',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_subject`(`subject_id` ASC) USING BTREE,
  INDEX `idx_teacher`(`teacher_id` ASC) USING BTREE,
  CONSTRAINT `course_ibfk_1` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `course_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, '高考数学函数专题突破', 1, '系统讲解函数的概念、性质、图像及导数的应用，帮助考生突破函数难点', NULL, NULL, 45, NULL, '张老师', 1257, 125, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');
INSERT INTO `course` VALUES (2, '三角函数解题技巧', 1, '掌握三角函数的核心公式和解题方法，提高解题速度和准确率', NULL, NULL, 30, NULL, '李老师', 892, 89, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');
INSERT INTO `course` VALUES (3, '数列求和方法总结', 1, '等差、等比数列及复杂数列的求和方法全解析', NULL, NULL, 35, NULL, '王老师', 756, 75, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');
INSERT INTO `course` VALUES (4, '高考语文古诗词鉴赏', 2, '从意象、手法、情感三个维度深入分析古诗词', NULL, NULL, 40, NULL, '陈老师', 1104, 110, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');
INSERT INTO `course` VALUES (5, '文言文翻译技巧', 2, '掌握文言文翻译的基本原则和常用技巧', NULL, NULL, 25, NULL, '刘老师', 678, 67, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');
INSERT INTO `course` VALUES (6, '高考英语语法精讲', 3, '系统梳理高考英语语法考点，突破语法难点', NULL, NULL, 50, NULL, '赵老师', 1532, 153, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');
INSERT INTO `course` VALUES (7, '英语阅读理解技巧', 3, '快速定位、推理判断、主旨大意等题型解题技巧', NULL, NULL, 35, NULL, '孙老师', 987, 98, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');
INSERT INTO `course` VALUES (8, '高考英语写作高分技巧', 3, '作文模板、高级词汇、句型升级全攻略', NULL, NULL, 28, NULL, '周老师', 845, 84, 3, 1, '2026-03-30 14:45:42', '2026-03-30 14:45:42');

-- ----------------------------
-- Table structure for course_progress
-- ----------------------------
DROP TABLE IF EXISTS `course_progress`;
CREATE TABLE `course_progress`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `progress` int NULL DEFAULT 0 COMMENT 'percentage 0-100',
  `last_position` int NULL DEFAULT 0 COMMENT 'seconds',
  `completed` tinyint NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_course`(`user_id` ASC, `course_id` ASC) USING BTREE,
  INDEX `course_id`(`course_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `course_progress_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `course_progress_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of course_progress
-- ----------------------------

-- ----------------------------
-- Table structure for exam_paper
-- ----------------------------
DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `subject_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `total_score` decimal(5, 1) NULL DEFAULT 100.0,
  `duration` int NULL DEFAULT 120 COMMENT 'minutes',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `status` tinyint NULL DEFAULT 1 COMMENT '0-draft, 1-published',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_subject`(`subject_id` ASC) USING BTREE,
  INDEX `idx_creator`(`creator_id` ASC) USING BTREE,
  CONSTRAINT `exam_paper_ibfk_1` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `exam_paper_ibfk_2` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exam_paper
-- ----------------------------
INSERT INTO `exam_paper` VALUES (1, '高考数学模拟试卷（一）', 1, 1, 100.0, 120, '高考数学模拟试卷，包含选择题、判断题和简答题', 1, '2026-04-01 09:58:02', '2026-04-01 09:58:02');
INSERT INTO `exam_paper` VALUES (2, '2025新高考一卷数学演练', 2, 1, 150.0, 120, '保留的2025新高考一卷数学题，按知识点完成标注后用于诊断练习', 1, '2026-05-26 10:57:40', '2026-05-30 18:46:01');
INSERT INTO `exam_paper` VALUES (6, '2025高考课标卷生物真题训练', 6, 1, 100.0, 90, '2025高考课标卷生物导入真题，含题图、答案和解析', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `exam_paper` VALUES (8, '2025全国卷理综化学真题训练', 5, 1, 100.0, 90, '2025全国卷理综化学导入真题，含题图、答案和解析', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `exam_paper` VALUES (9, '2025高考课标卷物理真题训练', 4, 1, 100.0, 90, '2025高考课标卷物理导入真题，含题图、答案和解析', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');

-- ----------------------------
-- Table structure for exam_question
-- ----------------------------
DROP TABLE IF EXISTS `exam_question`;
CREATE TABLE `exam_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `score` decimal(5, 1) NULL DEFAULT 10.0,
  `sort_order` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_question`(`exam_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `question_id`(`question_id` ASC) USING BTREE,
  INDEX `idx_exam`(`exam_id` ASC) USING BTREE,
  CONSTRAINT `exam_question_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exam_paper` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `exam_question_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 952 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exam_question
-- ----------------------------
INSERT INTO `exam_question` VALUES (225, 6, 84, 6.0, 1);
INSERT INTO `exam_question` VALUES (226, 6, 85, 6.0, 2);
INSERT INTO `exam_question` VALUES (227, 6, 86, 6.0, 3);
INSERT INTO `exam_question` VALUES (228, 6, 87, 6.0, 4);
INSERT INTO `exam_question` VALUES (229, 6, 88, 6.0, 5);
INSERT INTO `exam_question` VALUES (230, 6, 89, 6.0, 6);
INSERT INTO `exam_question` VALUES (231, 6, 90, 15.0, 7);
INSERT INTO `exam_question` VALUES (232, 6, 91, 15.0, 8);
INSERT INTO `exam_question` VALUES (233, 6, 92, 15.0, 9);
INSERT INTO `exam_question` VALUES (234, 6, 93, 15.0, 10);
INSERT INTO `exam_question` VALUES (235, 6, 94, 15.0, 11);
INSERT INTO `exam_question` VALUES (665, 8, 106, 6.0, 7);
INSERT INTO `exam_question` VALUES (666, 8, 107, 6.0, 8);
INSERT INTO `exam_question` VALUES (667, 8, 108, 6.0, 9);
INSERT INTO `exam_question` VALUES (668, 8, 109, 6.0, 10);
INSERT INTO `exam_question` VALUES (669, 8, 110, 6.0, 11);
INSERT INTO `exam_question` VALUES (670, 8, 111, 6.0, 12);
INSERT INTO `exam_question` VALUES (671, 8, 112, 6.0, 13);
INSERT INTO `exam_question` VALUES (672, 8, 113, 15.0, 26);
INSERT INTO `exam_question` VALUES (673, 8, 114, 15.0, 27);
INSERT INTO `exam_question` VALUES (674, 8, 115, 15.0, 28);
INSERT INTO `exam_question` VALUES (675, 8, 116, 15.0, 29);
INSERT INTO `exam_question` VALUES (698, 9, 117, 6.0, 1);
INSERT INTO `exam_question` VALUES (699, 9, 118, 6.0, 2);
INSERT INTO `exam_question` VALUES (700, 9, 119, 6.0, 3);
INSERT INTO `exam_question` VALUES (701, 9, 120, 6.0, 4);
INSERT INTO `exam_question` VALUES (702, 9, 121, 6.0, 5);
INSERT INTO `exam_question` VALUES (703, 9, 122, 6.0, 6);
INSERT INTO `exam_question` VALUES (704, 9, 123, 6.0, 7);
INSERT INTO `exam_question` VALUES (705, 9, 124, 6.0, 8);
INSERT INTO `exam_question` VALUES (706, 9, 125, 15.0, 9);
INSERT INTO `exam_question` VALUES (707, 9, 126, 15.0, 10);
INSERT INTO `exam_question` VALUES (708, 9, 127, 15.0, 11);
INSERT INTO `exam_question` VALUES (709, 9, 128, 15.0, 12);
INSERT INTO `exam_question` VALUES (930, 2, 12, 5.0, 1);
INSERT INTO `exam_question` VALUES (931, 2, 13, 5.0, 2);
INSERT INTO `exam_question` VALUES (932, 2, 14, 5.0, 3);
INSERT INTO `exam_question` VALUES (933, 2, 15, 5.0, 4);
INSERT INTO `exam_question` VALUES (934, 2, 16, 5.0, 5);
INSERT INTO `exam_question` VALUES (935, 2, 17, 5.0, 6);
INSERT INTO `exam_question` VALUES (936, 2, 18, 5.0, 7);
INSERT INTO `exam_question` VALUES (937, 2, 19, 5.0, 8);
INSERT INTO `exam_question` VALUES (938, 2, 20, 6.0, 9);
INSERT INTO `exam_question` VALUES (939, 2, 21, 6.0, 10);
INSERT INTO `exam_question` VALUES (940, 2, 22, 6.0, 11);
INSERT INTO `exam_question` VALUES (941, 2, 23, 6.0, 12);
INSERT INTO `exam_question` VALUES (942, 2, 24, 5.0, 13);
INSERT INTO `exam_question` VALUES (943, 2, 25, 5.0, 14);
INSERT INTO `exam_question` VALUES (944, 2, 26, 5.0, 15);
INSERT INTO `exam_question` VALUES (945, 2, 27, 5.0, 16);
INSERT INTO `exam_question` VALUES (946, 2, 44, 10.0, 17);
INSERT INTO `exam_question` VALUES (947, 2, 45, 12.0, 18);
INSERT INTO `exam_question` VALUES (948, 2, 46, 12.0, 19);
INSERT INTO `exam_question` VALUES (949, 2, 47, 12.0, 20);
INSERT INTO `exam_question` VALUES (950, 2, 48, 12.0, 21);
INSERT INTO `exam_question` VALUES (951, 2, 49, 12.0, 22);

-- ----------------------------
-- Table structure for exam_submission
-- ----------------------------
DROP TABLE IF EXISTS `exam_submission`;
CREATE TABLE `exam_submission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `total_score` decimal(5, 1) NULL DEFAULT 0.0,
  `ai_report` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI generated report',
  `submitted_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `status` tinyint NULL DEFAULT 1 COMMENT '0-in progress, 1-submitted, 2-graded',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_exam`(`exam_id` ASC) USING BTREE,
  CONSTRAINT `exam_submission_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `exam_submission_ibfk_2` FOREIGN KEY (`exam_id`) REFERENCES `exam_paper` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exam_submission
-- ----------------------------
INSERT INTO `exam_submission` VALUES (3, 2, 1, 0.0, NULL, '2026-04-01 10:02:14', 1);
INSERT INTO `exam_submission` VALUES (4, 2, 1, 10.0, NULL, '2026-04-01 10:10:11', 1);

-- ----------------------------
-- Table structure for knowledge_mastery
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_mastery`;
CREATE TABLE `knowledge_mastery`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `knowledge_id` bigint NOT NULL,
  `mastery_level` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '0-100 percentage',
  `total_questions` int NULL DEFAULT 0,
  `correct_questions` int NULL DEFAULT 0,
  `last_practice_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_knowledge`(`user_id` ASC, `knowledge_id` ASC) USING BTREE,
  INDEX `knowledge_id`(`knowledge_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_mastery`(`mastery_level` ASC) USING BTREE,
  CONSTRAINT `knowledge_mastery_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `knowledge_mastery_ibfk_2` FOREIGN KEY (`knowledge_id`) REFERENCES `knowledge_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 84 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_mastery
-- ----------------------------
INSERT INTO `knowledge_mastery` VALUES (36, 6, 16, 28.57, 7, 2, '2026-05-27 11:31:45', '2026-05-26 11:05:17', '2026-05-26 11:05:17');
INSERT INTO `knowledge_mastery` VALUES (37, 6, 76, 40.00, 10, 4, '2026-05-27 11:27:55', '2026-05-26 11:05:29', '2026-05-26 11:05:29');
INSERT INTO `knowledge_mastery` VALUES (38, 6, 75, 40.00, 5, 2, '2026-05-27 09:20:19', '2026-05-26 11:05:45', '2026-05-26 11:05:45');
INSERT INTO `knowledge_mastery` VALUES (39, 6, 17, 50.00, 10, 5, '2026-05-27 11:31:17', '2026-05-26 11:05:54', '2026-05-26 11:05:54');
INSERT INTO `knowledge_mastery` VALUES (40, 6, 15, 45.45, 11, 5, '2026-05-28 08:56:14', '2026-05-26 11:06:10', '2026-05-26 11:06:10');
INSERT INTO `knowledge_mastery` VALUES (41, 6, 14, 25.00, 12, 3, '2026-05-27 11:31:41', '2026-05-26 11:06:26', '2026-05-26 11:06:26');
INSERT INTO `knowledge_mastery` VALUES (42, 6, 74, 10.00, 10, 1, '2026-05-28 08:56:11', '2026-05-26 11:06:31', '2026-05-26 11:06:31');
INSERT INTO `knowledge_mastery` VALUES (43, 6, 18, 28.57, 7, 2, '2026-05-27 09:20:07', '2026-05-26 11:06:37', '2026-05-26 11:06:37');
INSERT INTO `knowledge_mastery` VALUES (44, 6, 77, 50.00, 2, 1, '2026-05-26 17:05:34', '2026-05-26 11:45:23', '2026-05-26 11:45:23');
INSERT INTO `knowledge_mastery` VALUES (45, 6, 78, 16.67, 6, 1, '2026-05-28 08:56:16', '2026-05-26 11:45:27', '2026-05-26 11:45:27');
INSERT INTO `knowledge_mastery` VALUES (66, 6, 108, 12.50, 8, 1, '2026-05-27 14:22:51', '2026-05-27 08:54:28', '2026-05-27 08:54:28');
INSERT INTO `knowledge_mastery` VALUES (67, 6, 109, 50.00, 6, 3, '2026-05-27 14:24:14', '2026-05-27 08:54:32', '2026-05-27 08:54:32');
INSERT INTO `knowledge_mastery` VALUES (68, 6, 110, 25.00, 8, 2, '2026-05-27 14:22:46', '2026-05-27 08:54:40', '2026-05-27 08:54:40');
INSERT INTO `knowledge_mastery` VALUES (69, 6, 107, 20.00, 5, 1, '2026-05-27 14:22:36', '2026-05-27 08:55:00', '2026-05-27 08:55:00');
INSERT INTO `knowledge_mastery` VALUES (70, 6, 111, 50.00, 4, 2, '2026-05-27 14:18:46', '2026-05-27 08:55:02', '2026-05-27 08:55:02');
INSERT INTO `knowledge_mastery` VALUES (71, 6, 104, 50.00, 2, 1, '2026-05-27 09:19:12', '2026-05-27 08:57:04', '2026-05-27 08:57:04');
INSERT INTO `knowledge_mastery` VALUES (72, 6, 106, 33.33, 3, 1, '2026-05-27 09:19:15', '2026-05-27 08:57:07', '2026-05-27 08:57:07');
INSERT INTO `knowledge_mastery` VALUES (73, 6, 105, 0.00, 2, 0, '2026-05-27 09:19:09', '2026-05-27 08:57:16', '2026-05-27 08:57:16');
INSERT INTO `knowledge_mastery` VALUES (74, 6, 114, 0.00, 1, 0, '2026-05-28 09:28:09', '2026-05-28 09:28:09', '2026-05-28 09:28:09');
INSERT INTO `knowledge_mastery` VALUES (75, 6, 113, 0.00, 2, 0, '2026-05-28 09:28:18', '2026-05-28 09:28:12', '2026-05-28 09:28:12');
INSERT INTO `knowledge_mastery` VALUES (76, 6, 112, 0.00, 1, 0, '2026-05-28 09:28:14', '2026-05-28 09:28:14', '2026-05-28 09:28:14');
INSERT INTO `knowledge_mastery` VALUES (77, 6, 116, 0.00, 2, 0, '2026-05-28 09:28:21', '2026-05-28 09:28:16', '2026-05-28 09:28:16');
INSERT INTO `knowledge_mastery` VALUES (78, 6, 115, 0.00, 1, 0, '2026-05-28 09:28:24', '2026-05-28 09:28:24', '2026-05-28 09:28:24');
INSERT INTO `knowledge_mastery` VALUES (79, 6, 122, 0.00, 2, 0, '2026-05-28 09:56:13', '2026-05-28 09:56:00', '2026-05-28 09:56:00');
INSERT INTO `knowledge_mastery` VALUES (80, 6, 120, 50.00, 2, 1, '2026-05-28 09:56:15', '2026-05-28 09:56:02', '2026-05-28 09:56:02');
INSERT INTO `knowledge_mastery` VALUES (81, 6, 123, 0.00, 1, 0, '2026-05-28 09:56:05', '2026-05-28 09:56:05', '2026-05-28 09:56:05');
INSERT INTO `knowledge_mastery` VALUES (82, 6, 121, 0.00, 1, 0, '2026-05-28 09:56:08', '2026-05-28 09:56:08', '2026-05-28 09:56:08');
INSERT INTO `knowledge_mastery` VALUES (83, 6, 119, 0.00, 1, 0, '2026-05-28 09:56:10', '2026-05-28 09:56:10', '2026-05-28 09:56:10');

-- ----------------------------
-- Table structure for knowledge_point
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_point`;
CREATE TABLE `knowledge_point`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` bigint NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `parent_id` bigint NULL DEFAULT 0,
  `level` int NULL DEFAULT 1,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_subject`(`subject_id` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `knowledge_point_ibfk_1` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_point
-- ----------------------------
INSERT INTO `knowledge_point` VALUES (14, 2, '函数与导数', 0, 1, '函数的概念、性质及导数的应用', '2026-05-20 09:31:32');
INSERT INTO `knowledge_point` VALUES (15, 2, '三角函数', 0, 1, '正弦、余弦、正切函数及其应用', '2026-05-20 09:31:32');
INSERT INTO `knowledge_point` VALUES (16, 2, '数列', 0, 1, '等差数列、等比数列', '2026-05-20 09:31:32');
INSERT INTO `knowledge_point` VALUES (17, 2, '立体几何', 0, 1, '空间几何体的结构特征', '2026-05-20 09:31:32');
INSERT INTO `knowledge_point` VALUES (18, 2, '解析几何', 0, 1, '直线、圆、圆锥曲线', '2026-05-20 09:31:32');
INSERT INTO `knowledge_point` VALUES (74, 2, '概率统计', 0, 1, '随机变量、概率模型与统计分析', '2026-05-26 09:02:04');
INSERT INTO `knowledge_point` VALUES (75, 2, '平面向量', 0, 1, '向量运算、数量积与几何应用', '2026-05-26 09:02:04');
INSERT INTO `knowledge_point` VALUES (76, 2, '不等式', 0, 1, '不等式求解、恒成立与线性规划', '2026-05-26 09:02:04');
INSERT INTO `knowledge_point` VALUES (77, 2, '集合与常用逻辑', 0, 1, '集合运算、命题与常用逻辑', '2026-05-26 09:02:04');
INSERT INTO `knowledge_point` VALUES (78, 2, '复数', 0, 1, '复数运算与几何意义', '2026-05-26 09:02:04');
INSERT INTO `knowledge_point` VALUES (104, 6, '生命的物质基础', 0, 1, '蛋白质、核酸等生命大分子的结构与功能', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (105, 6, '细胞代谢', 0, 1, '光合作用、呼吸作用和物质能量变化', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (106, 6, '生命活动调节', 0, 1, '神经调节、体液调节和激素调节', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (107, 6, '遗传与育种', 0, 1, '遗传规律、变异、育种和性状分析', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (108, 6, '生态系统', 0, 1, '种群、群落、生态系统结构与稳定性', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (109, 6, '免疫调节', 0, 1, '免疫系统、免疫失调和免疫应答', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (110, 6, '生物技术与实验', 0, 1, 'PCR、电泳、基因工程和实验设计', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (111, 6, '细胞结构与物质运输', 0, 1, '细胞结构、渗透作用和跨膜运输', '2026-05-27 08:53:56');
INSERT INTO `knowledge_point` VALUES (112, 5, '化学与生活', 0, 1, '材料、食品、环境和生活中的化学应用', '2026-05-28 09:12:47');
INSERT INTO `knowledge_point` VALUES (113, 5, '电化学', 0, 1, '原电池、电解池、金属腐蚀与防护', '2026-05-28 09:12:47');
INSERT INTO `knowledge_point` VALUES (114, 5, '有机化学', 0, 1, '有机物结构、性质、合成路线与反应类型', '2026-05-28 09:12:47');
INSERT INTO `knowledge_point` VALUES (115, 5, '物质结构与元素周期律', 0, 1, '原子结构、元素周期律、化学键与分子结构', '2026-05-28 09:12:47');
INSERT INTO `knowledge_point` VALUES (116, 5, '化学反应原理', 0, 1, '反应速率、化学平衡、热化学与溶液平衡', '2026-05-28 09:12:47');
INSERT INTO `knowledge_point` VALUES (117, 5, '化学实验', 0, 1, '实验方案、现象分析、仪器与操作', '2026-05-28 09:12:47');
INSERT INTO `knowledge_point` VALUES (118, 5, '化学计算', 0, 1, '物质的量、平衡常数和综合定量分析', '2026-05-28 09:12:47');
INSERT INTO `knowledge_point` VALUES (119, 4, '运动学与动力学', 0, 1, '直线运动、牛顿运动定律和受力分析', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (120, 4, '万有引力与航天', 0, 1, '天体运动、卫星轨道和引力规律', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (121, 4, '功和能', 0, 1, '动能定理、机械能守恒和能量转化', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (122, 4, '电场', 0, 1, '电势、电场强度和带电粒子运动', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (123, 4, '磁场', 0, 1, '洛伦兹力、带电粒子在磁场中的运动', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (124, 4, '热学', 0, 1, '理想气体、状态参量和热力学图像', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (125, 4, '波动与振动', 0, 1, '机械波、振动和图像分析', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (126, 4, '电学实验', 0, 1, '电路测量、传感器和实验数据处理', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (127, 4, '电容器', 0, 1, '电容器、电介质和电场能量', '2026-05-28 09:45:57');
INSERT INTO `knowledge_point` VALUES (128, 4, '动量与机械能', 0, 1, '碰撞、弹簧模型和综合力学', '2026-05-28 09:45:57');

-- ----------------------------
-- Table structure for learning_stats
-- ----------------------------
DROP TABLE IF EXISTS `learning_stats`;
CREATE TABLE `learning_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `date` date NOT NULL,
  `study_time` int NULL DEFAULT 0 COMMENT 'minutes',
  `questions_answered` int NULL DEFAULT 0,
  `correct_count` int NULL DEFAULT 0,
  `courses_watched` int NULL DEFAULT 0,
  `notes_created` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `date` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_date`(`date` ASC) USING BTREE,
  CONSTRAINT `learning_stats_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of learning_stats
-- ----------------------------

-- ----------------------------
-- Table structure for like_record
-- ----------------------------
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_type` enum('POST','COMMENT','NOTE','COURSE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_target`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  CONSTRAINT `like_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of like_record
-- ----------------------------
INSERT INTO `like_record` VALUES (3, 2, 'POST', 1, '2026-03-30 17:05:55');
INSERT INTO `like_record` VALUES (6, 4, 'NOTE', 3, '2026-03-30 17:23:40');
INSERT INTO `like_record` VALUES (9, 6, 'NOTE', 4, '2026-05-27 10:04:22');
INSERT INTO `like_record` VALUES (13, 6, 'NOTE', 5, '2026-05-28 11:30:39');

-- ----------------------------
-- Table structure for mistake_book
-- ----------------------------
DROP TABLE IF EXISTS `mistake_book`;
CREATE TABLE `mistake_book`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `mistake_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `review_count` int NULL DEFAULT 0,
  `last_review_at` datetime NULL DEFAULT NULL,
  `next_review_date` date NULL DEFAULT NULL,
  `mastery_level` tinyint NULL DEFAULT 0 COMMENT '0-5, 0=not mastered, 5=fully mastered',
  `note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_question`(`user_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `question_id`(`question_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_next_review`(`next_review_date` ASC) USING BTREE,
  CONSTRAINT `mistake_book_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `mistake_book_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 106 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mistake_book
-- ----------------------------
INSERT INTO `mistake_book` VALUES (19, 10, 22, 'CONCEPT_ERROR', 0, NULL, '2026-05-21', 0, NULL, '2026-05-20 10:03:27');
INSERT INTO `mistake_book` VALUES (45, 6, 20, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:05:17');
INSERT INTO `mistake_book` VALUES (46, 6, 49, 'UNDERSTANDING_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:05:29');
INSERT INTO `mistake_book` VALUES (47, 6, 14, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:05:45');
INSERT INTO `mistake_book` VALUES (48, 6, 46, 'UNDERSTANDING_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:05:54');
INSERT INTO `mistake_book` VALUES (49, 6, 16, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:06:10');
INSERT INTO `mistake_book` VALUES (50, 6, 44, 'UNDERSTANDING_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:06:17');
INSERT INTO `mistake_book` VALUES (51, 6, 25, 'MEMORY_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:06:26');
INSERT INTO `mistake_book` VALUES (52, 6, 19, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:06:31');
INSERT INTO `mistake_book` VALUES (53, 6, 23, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:06:37');
INSERT INTO `mistake_book` VALUES (54, 6, 22, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:06:42');
INSERT INTO `mistake_book` VALUES (55, 6, 15, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:45:01');
INSERT INTO `mistake_book` VALUES (56, 6, 18, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:45:15');
INSERT INTO `mistake_book` VALUES (57, 6, 12, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:45:23');
INSERT INTO `mistake_book` VALUES (58, 6, 13, 'CONCEPT_ERROR', 0, NULL, '2026-05-27', 0, NULL, '2026-05-26 11:45:27');
INSERT INTO `mistake_book` VALUES (80, 6, 92, 'UNDERSTANDING_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:54:28');
INSERT INTO `mistake_book` VALUES (81, 6, 91, 'UNDERSTANDING_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:54:32');
INSERT INTO `mistake_book` VALUES (82, 6, 45, 'UNDERSTANDING_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:54:36');
INSERT INTO `mistake_book` VALUES (83, 6, 86, 'CONCEPT_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:57:07');
INSERT INTO `mistake_book` VALUES (84, 6, 88, 'CONCEPT_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:57:10');
INSERT INTO `mistake_book` VALUES (85, 6, 89, 'CONCEPT_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:57:13');
INSERT INTO `mistake_book` VALUES (86, 6, 85, 'CONCEPT_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:57:16');
INSERT INTO `mistake_book` VALUES (87, 6, 87, 'CONCEPT_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 08:57:19');
INSERT INTO `mistake_book` VALUES (88, 6, 84, 'CONCEPT_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 09:19:12');
INSERT INTO `mistake_book` VALUES (89, 6, 17, 'CONCEPT_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 09:19:38');
INSERT INTO `mistake_book` VALUES (90, 6, 47, 'UNDERSTANDING_ERROR', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 11:31:41');
INSERT INTO `mistake_book` VALUES (91, 6, 90, 'INCOMPLETE', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 14:18:29');
INSERT INTO `mistake_book` VALUES (92, 6, 93, 'INCOMPLETE', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 14:18:32');
INSERT INTO `mistake_book` VALUES (93, 6, 94, 'INCOMPLETE', 0, NULL, '2026-05-28', 0, NULL, '2026-05-27 14:18:51');
INSERT INTO `mistake_book` VALUES (94, 6, 108, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:28:09');
INSERT INTO `mistake_book` VALUES (95, 6, 109, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:28:12');
INSERT INTO `mistake_book` VALUES (96, 6, 106, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:28:14');
INSERT INTO `mistake_book` VALUES (97, 6, 111, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:28:16');
INSERT INTO `mistake_book` VALUES (98, 6, 107, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:28:18');
INSERT INTO `mistake_book` VALUES (99, 6, 112, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:28:21');
INSERT INTO `mistake_book` VALUES (100, 6, 110, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:28:24');
INSERT INTO `mistake_book` VALUES (101, 6, 120, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:56:00');
INSERT INTO `mistake_book` VALUES (102, 6, 121, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:56:05');
INSERT INTO `mistake_book` VALUES (103, 6, 119, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:56:08');
INSERT INTO `mistake_book` VALUES (104, 6, 117, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:56:10');
INSERT INTO `mistake_book` VALUES (105, 6, 118, 'CONCEPT_ERROR', 0, NULL, '2026-05-29', 0, NULL, '2026-05-28 09:56:15');

-- ----------------------------
-- Table structure for post_comment
-- ----------------------------
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint NULL DEFAULT 0 COMMENT 'for replies',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `like_count` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post`(`post_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `post_comment_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `post_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of post_comment
-- ----------------------------
INSERT INTO `post_comment` VALUES (1, 1, 2, 0, '还真是', 0, '2026-03-30 15:55:14');
INSERT INTO `post_comment` VALUES (2, 1, 2, 0, '还真不是\n', 0, '2026-03-30 15:57:25');
INSERT INTO `post_comment` VALUES (3, 1, 2, 0, '是个蛋\n', 0, '2026-03-30 16:12:09');
INSERT INTO `post_comment` VALUES (4, 1, 4, 0, '是\n', 0, '2026-03-30 16:12:45');
INSERT INTO `post_comment` VALUES (5, 3, 6, 0, '塞布', 0, '2026-05-26 08:38:15');

-- ----------------------------
-- Table structure for practice_answer
-- ----------------------------
DROP TABLE IF EXISTS `practice_answer`;
CREATE TABLE `practice_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `practice_record_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `question_order` int NULL DEFAULT 0,
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_correct` tinyint NULL DEFAULT NULL COMMENT '0-wrong, 1-correct',
  `score` decimal(5, 2) NULL DEFAULT 0.00,
  `mistake_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ai_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `answer_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_practice_record`(`practice_record_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_question`(`question_id` ASC) USING BTREE,
  CONSTRAINT `practice_answer_ibfk_1` FOREIGN KEY (`practice_record_id`) REFERENCES `practice_record` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `practice_answer_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `practice_answer_ibfk_3` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 218 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of practice_answer
-- ----------------------------
INSERT INTO `practice_answer` VALUES (1, 1, 10, 22, 1, 'A,B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-20 10:03:27');
INSERT INTO `practice_answer` VALUES (71, 16, 6, 20, 1, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:05:17');
INSERT INTO `practice_answer` VALUES (72, 16, 6, 49, 2, 'll', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-26 11:05:29');
INSERT INTO `practice_answer` VALUES (73, 16, 6, 14, 3, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:05:45');
INSERT INTO `practice_answer` VALUES (74, 16, 6, 46, 4, '来了，', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-26 11:05:54');
INSERT INTO `practice_answer` VALUES (77, 16, 6, 16, 5, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:06:10');
INSERT INTO `practice_answer` VALUES (78, 16, 6, 44, 6, '；，；l', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-26 11:06:17');
INSERT INTO `practice_answer` VALUES (79, 16, 6, 25, 7, '-1', 0, 0.00, 'MEMORY_ERROR', NULL, '2026-05-26 11:06:26');
INSERT INTO `practice_answer` VALUES (80, 16, 6, 19, 8, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:06:31');
INSERT INTO `practice_answer` VALUES (81, 16, 6, 23, 9, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:06:37');
INSERT INTO `practice_answer` VALUES (82, 16, 6, 22, 10, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:06:42');
INSERT INTO `practice_answer` VALUES (83, 17, 6, 15, 1, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:45:01');
INSERT INTO `practice_answer` VALUES (84, 17, 6, 14, 2, 'B', 1, 10.00, NULL, NULL, '2026-05-26 11:45:08');
INSERT INTO `practice_answer` VALUES (85, 17, 6, 18, 3, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:45:15');
INSERT INTO `practice_answer` VALUES (86, 17, 6, 16, 4, 'A', 1, 10.00, NULL, NULL, '2026-05-26 11:45:19');
INSERT INTO `practice_answer` VALUES (87, 17, 6, 12, 5, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:45:23');
INSERT INTO `practice_answer` VALUES (88, 17, 6, 13, 6, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:45:27');
INSERT INTO `practice_answer` VALUES (89, 17, 6, 17, 7, 'A', 1, 10.00, NULL, NULL, '2026-05-26 11:45:32');
INSERT INTO `practice_answer` VALUES (90, 17, 6, 19, 8, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 11:45:38');
INSERT INTO `practice_answer` VALUES (91, 19, 6, 25, 1, '1', 0, 0.00, 'MEMORY_ERROR', NULL, '2026-05-26 17:04:39');
INSERT INTO `practice_answer` VALUES (92, 19, 6, 49, 2, '1', 1, 10.00, NULL, NULL, '2026-05-26 17:04:41');
INSERT INTO `practice_answer` VALUES (93, 19, 6, 44, 3, '1', 1, 10.00, NULL, NULL, '2026-05-26 17:05:02');
INSERT INTO `practice_answer` VALUES (94, 19, 6, 19, 4, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:05');
INSERT INTO `practice_answer` VALUES (95, 19, 6, 16, 5, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:07');
INSERT INTO `practice_answer` VALUES (96, 19, 6, 22, 6, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:12');
INSERT INTO `practice_answer` VALUES (97, 19, 6, 20, 7, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:16');
INSERT INTO `practice_answer` VALUES (98, 19, 6, 23, 8, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:18');
INSERT INTO `practice_answer` VALUES (99, 19, 6, 46, 9, '1', 1, 10.00, NULL, NULL, '2026-05-26 17:05:20');
INSERT INTO `practice_answer` VALUES (100, 19, 6, 14, 10, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:25');
INSERT INTO `practice_answer` VALUES (101, 20, 6, 12, 1, 'A', 1, 10.00, NULL, NULL, '2026-05-26 17:05:34');
INSERT INTO `practice_answer` VALUES (102, 20, 6, 15, 2, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:37');
INSERT INTO `practice_answer` VALUES (103, 20, 6, 19, 3, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:39');
INSERT INTO `practice_answer` VALUES (104, 20, 6, 18, 4, 'A', 1, 10.00, NULL, NULL, '2026-05-26 17:05:42');
INSERT INTO `practice_answer` VALUES (105, 20, 6, 13, 5, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:05:45');
INSERT INTO `practice_answer` VALUES (106, 21, 6, 19, 1, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:06:02');
INSERT INTO `practice_answer` VALUES (107, 21, 6, 15, 2, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:06:05');
INSERT INTO `practice_answer` VALUES (108, 21, 6, 13, 3, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:06:08');
INSERT INTO `practice_answer` VALUES (109, 22, 6, 13, 1, 'A', 1, 10.00, NULL, NULL, '2026-05-26 17:06:14');
INSERT INTO `practice_answer` VALUES (110, 22, 6, 15, 2, 'B', 1, 10.00, NULL, NULL, '2026-05-26 17:06:17');
INSERT INTO `practice_answer` VALUES (111, 22, 6, 19, 3, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-26 17:06:20');
INSERT INTO `practice_answer` VALUES (112, 23, 6, 19, 1, 'B', 1, 10.00, NULL, NULL, '2026-05-26 17:06:32');
INSERT INTO `practice_answer` VALUES (132, 26, 6, 92, 1, 'erthj', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-27 08:54:28');
INSERT INTO `practice_answer` VALUES (133, 26, 6, 91, 2, '玩y', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-27 08:54:32');
INSERT INTO `practice_answer` VALUES (134, 26, 6, 45, 3, 'qawer', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-27 08:54:36');
INSERT INTO `practice_answer` VALUES (135, 26, 6, 94, 4, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:54:40');
INSERT INTO `practice_answer` VALUES (136, 26, 6, 47, 5, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:54:55');
INSERT INTO `practice_answer` VALUES (137, 26, 6, 48, 6, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:54:57');
INSERT INTO `practice_answer` VALUES (138, 26, 6, 93, 7, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:55:00');
INSERT INTO `practice_answer` VALUES (139, 26, 6, 90, 8, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:55:02');
INSERT INTO `practice_answer` VALUES (140, 27, 6, 84, 1, 'A', 1, 10.00, NULL, NULL, '2026-05-27 08:57:04');
INSERT INTO `practice_answer` VALUES (141, 27, 6, 86, 2, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 08:57:07');
INSERT INTO `practice_answer` VALUES (142, 27, 6, 88, 3, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 08:57:10');
INSERT INTO `practice_answer` VALUES (143, 27, 6, 89, 4, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 08:57:13');
INSERT INTO `practice_answer` VALUES (144, 27, 6, 85, 5, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 08:57:16');
INSERT INTO `practice_answer` VALUES (145, 27, 6, 87, 6, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 08:57:19');
INSERT INTO `practice_answer` VALUES (146, 28, 6, 91, 1, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:57:35');
INSERT INTO `practice_answer` VALUES (147, 28, 6, 45, 2, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:57:42');
INSERT INTO `practice_answer` VALUES (148, 28, 6, 92, 3, '1', 1, 10.00, NULL, NULL, '2026-05-27 08:57:45');
INSERT INTO `practice_answer` VALUES (149, 30, 6, 90, 1, '1', 1, 10.00, NULL, NULL, '2026-05-27 09:18:34');
INSERT INTO `practice_answer` VALUES (150, 30, 6, 91, 2, '1', 1, 10.00, NULL, NULL, '2026-05-27 09:18:36');
INSERT INTO `practice_answer` VALUES (151, 30, 6, 94, 3, '2', 1, 10.00, NULL, NULL, '2026-05-27 09:18:40');
INSERT INTO `practice_answer` VALUES (152, 31, 6, 86, 1, 'C', 1, 10.00, NULL, NULL, '2026-05-27 09:18:58');
INSERT INTO `practice_answer` VALUES (153, 31, 6, 87, 2, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:02');
INSERT INTO `practice_answer` VALUES (154, 31, 6, 88, 3, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:05');
INSERT INTO `practice_answer` VALUES (155, 31, 6, 89, 4, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:07');
INSERT INTO `practice_answer` VALUES (156, 31, 6, 85, 5, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:09');
INSERT INTO `practice_answer` VALUES (157, 31, 6, 84, 6, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:12');
INSERT INTO `practice_answer` VALUES (158, 31, 6, 86, 7, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:15');
INSERT INTO `practice_answer` VALUES (159, 32, 6, 15, 1, 'B', 1, 10.00, NULL, NULL, '2026-05-27 09:19:24');
INSERT INTO `practice_answer` VALUES (160, 32, 6, 13, 2, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:28');
INSERT INTO `practice_answer` VALUES (161, 32, 6, 16, 3, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:30');
INSERT INTO `practice_answer` VALUES (162, 32, 6, 14, 4, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:32');
INSERT INTO `practice_answer` VALUES (163, 32, 6, 18, 5, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:34');
INSERT INTO `practice_answer` VALUES (164, 32, 6, 19, 6, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:37');
INSERT INTO `practice_answer` VALUES (165, 32, 6, 17, 7, 'D', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:19:38');
INSERT INTO `practice_answer` VALUES (166, 33, 6, 49, 1, '1', 1, 10.00, NULL, NULL, '2026-05-27 09:20:05');
INSERT INTO `practice_answer` VALUES (167, 33, 6, 23, 2, 'A', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:20:07');
INSERT INTO `practice_answer` VALUES (168, 33, 6, 22, 3, 'CD', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:20:10');
INSERT INTO `practice_answer` VALUES (169, 33, 6, 20, 4, 'B', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:20:12');
INSERT INTO `practice_answer` VALUES (170, 33, 6, 16, 5, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:20:14');
INSERT INTO `practice_answer` VALUES (171, 33, 6, 44, 6, '1', 1, 10.00, NULL, NULL, '2026-05-27 09:20:16');
INSERT INTO `practice_answer` VALUES (172, 33, 6, 25, 7, '1', 0, 0.00, 'MEMORY_ERROR', NULL, '2026-05-27 09:20:18');
INSERT INTO `practice_answer` VALUES (173, 33, 6, 14, 8, 'B', 1, 10.00, NULL, NULL, '2026-05-27 09:20:19');
INSERT INTO `practice_answer` VALUES (174, 33, 6, 46, 9, '2', 1, 10.00, NULL, NULL, '2026-05-27 09:20:21');
INSERT INTO `practice_answer` VALUES (175, 33, 6, 19, 10, 'C', 0, 0.00, 'CONCEPT_ERROR', NULL, '2026-05-27 09:20:23');
INSERT INTO `practice_answer` VALUES (176, 34, 6, 25, 1, '1', 0, 0.00, 'MEMORY_ERROR', NULL, '2026-05-27 11:27:24');
INSERT INTO `practice_answer` VALUES (177, 35, 6, 46, 1, '1', 1, 10.00, NULL, NULL, '2026-05-27 11:27:50');
INSERT INTO `practice_answer` VALUES (178, 35, 6, 44, 2, '2', 1, 10.00, NULL, NULL, '2026-05-27 11:27:53');
INSERT INTO `practice_answer` VALUES (179, 35, 6, 49, 3, '3', 1, 10.00, NULL, NULL, '2026-05-27 11:27:55');
INSERT INTO `practice_answer` VALUES (180, 36, 6, 46, 1, '0', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-27 11:31:17');
INSERT INTO `practice_answer` VALUES (181, 36, 6, 45, 2, '3', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-27 11:31:25');
INSERT INTO `practice_answer` VALUES (182, 36, 6, 47, 3, '2', 1, 10.00, NULL, NULL, '2026-05-27 11:31:29');
INSERT INTO `practice_answer` VALUES (183, 37, 6, 47, 1, '12', 0, 0.00, 'UNDERSTANDING_ERROR', NULL, '2026-05-27 11:31:41');
INSERT INTO `practice_answer` VALUES (184, 37, 6, 45, 2, '2', 1, 10.00, NULL, NULL, '2026-05-27 11:31:45');
INSERT INTO `practice_answer` VALUES (185, 37, 6, 44, 3, '2', 1, 10.00, NULL, NULL, '2026-05-27 11:31:47');
INSERT INTO `practice_answer` VALUES (186, 38, 6, 91, 1, '1', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:18:16');
INSERT INTO `practice_answer` VALUES (187, 38, 6, 90, 2, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:18:29');
INSERT INTO `practice_answer` VALUES (188, 38, 6, 93, 3, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:18:32');
INSERT INTO `practice_answer` VALUES (189, 39, 6, 92, 1, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:18:40');
INSERT INTO `practice_answer` VALUES (190, 39, 6, 90, 2, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:18:46');
INSERT INTO `practice_answer` VALUES (191, 39, 6, 94, 3, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:18:51');
INSERT INTO `practice_answer` VALUES (192, 40, 6, 91, 1, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 1, 10.00, NULL, 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:19:00');
INSERT INTO `practice_answer` VALUES (193, 40, 6, 92, 2, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:19:28');
INSERT INTO `practice_answer` VALUES (194, 40, 6, 94, 3, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:19:36');
INSERT INTO `practice_answer` VALUES (195, 42, 6, 92, 1, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:22:34');
INSERT INTO `practice_answer` VALUES (196, 42, 6, 93, 2, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:22:36');
INSERT INTO `practice_answer` VALUES (197, 42, 6, 94, 3, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:22:40');
INSERT INTO `practice_answer` VALUES (198, 43, 6, 94, 1, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:22:46');
INSERT INTO `practice_answer` VALUES (199, 43, 6, 92, 2, '（1）自身免疫病、免疫缺陷病\n（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量\n（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:22:51');
INSERT INTO `practice_answer` VALUES (200, 43, 6, 91, 3, '（1）自身免疫病、免疫缺陷病\n（2）抑制 、 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量的改变\n（3）酶E 、再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 0, 0.00, 'INCOMPLETE', 'AI批改暂不可用，系统已使用严格兜底判分：AI服务未配置API Key，请设置 claude.api-key 或 DEEPSEEK_API_KEY', '2026-05-27 14:24:14');
INSERT INTO `practice_answer` VALUES (201, 45, 6, 19, 1, 'D', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 08:56:11');
INSERT INTO `practice_answer` VALUES (202, 45, 6, 16, 2, 'D', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 08:56:14');
INSERT INTO `practice_answer` VALUES (203, 45, 6, 13, 3, 'D', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 08:56:16');
INSERT INTO `practice_answer` VALUES (204, 46, 6, 108, 1, 'B', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:28:09');
INSERT INTO `practice_answer` VALUES (205, 46, 6, 109, 2, 'C', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:28:12');
INSERT INTO `practice_answer` VALUES (206, 46, 6, 106, 3, 'A', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:28:14');
INSERT INTO `practice_answer` VALUES (207, 46, 6, 111, 4, 'C', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:28:16');
INSERT INTO `practice_answer` VALUES (208, 46, 6, 107, 5, 'D', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:28:18');
INSERT INTO `practice_answer` VALUES (209, 46, 6, 112, 6, 'A', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:28:21');
INSERT INTO `practice_answer` VALUES (210, 46, 6, 110, 7, 'C', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:28:24');
INSERT INTO `practice_answer` VALUES (211, 47, 6, 120, 1, 'D', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:56:00');
INSERT INTO `practice_answer` VALUES (212, 47, 6, 118, 2, 'C', 1, 10.00, NULL, '回答正确。', '2026-05-28 09:56:02');
INSERT INTO `practice_answer` VALUES (213, 47, 6, 121, 3, 'B', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:56:05');
INSERT INTO `practice_answer` VALUES (214, 47, 6, 119, 4, 'A', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:56:08');
INSERT INTO `practice_answer` VALUES (215, 47, 6, 117, 5, 'C', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:56:10');
INSERT INTO `practice_answer` VALUES (216, 47, 6, 120, 6, 'A', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:56:13');
INSERT INTO `practice_answer` VALUES (217, 47, 6, 118, 7, 'D', 0, 0.00, 'CONCEPT_ERROR', '答案错误，请结合解析订正。', '2026-05-28 09:56:15');

-- ----------------------------
-- Table structure for practice_record
-- ----------------------------
DROP TABLE IF EXISTS `practice_record`;
CREATE TABLE `practice_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `practice_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'UUID练习ID',
  `practice_type` enum('SUBJECT','KNOWLEDGE','TYPE','DIFFICULTY','MISTAKE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `subject_id` bigint NULL DEFAULT NULL,
  `knowledge_id` bigint NULL DEFAULT NULL,
  `question_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `difficulty` tinyint NULL DEFAULT NULL,
  `total_questions` int NULL DEFAULT 0,
  `correct_count` int NULL DEFAULT 0,
  `accuracy_rate` decimal(5, 2) NULL DEFAULT 0.00,
  `total_score` decimal(10, 2) NULL DEFAULT 0.00,
  `duration` int NULL DEFAULT 0 COMMENT 'seconds',
  `status` tinyint NULL DEFAULT 0 COMMENT '0-in progress, 1-completed',
  `start_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `practice_id`(`practice_id` ASC) USING BTREE,
  INDEX `subject_id`(`subject_id` ASC) USING BTREE,
  INDEX `knowledge_id`(`knowledge_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_practice_id`(`practice_id` ASC) USING BTREE,
  INDEX `idx_type`(`practice_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `practice_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `practice_record_ibfk_2` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `practice_record_ibfk_3` FOREIGN KEY (`knowledge_id`) REFERENCES `knowledge_point` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of practice_record
-- ----------------------------
INSERT INTO `practice_record` VALUES (1, 10, 'ad6f45ad49804c1faff2d5a3316f0e23', 'SUBJECT', 2, NULL, NULL, NULL, 10, 0, 0.00, 0.00, 100, 1, '2026-05-20 10:02:39', '2026-05-20 10:04:20');
INSERT INTO `practice_record` VALUES (16, 6, '93add66000314a38ae582058deedd7a9', 'SUBJECT', 2, NULL, NULL, NULL, 10, 0, 0.00, 0.00, 87, 1, '2026-05-26 11:05:15', '2026-05-26 11:06:43');
INSERT INTO `practice_record` VALUES (17, 6, '00ca5baa480e4b09a33f73c586257de1', 'TYPE', 2, NULL, 'SINGLE_CHOICE', NULL, 8, 3, 37.50, 30.00, 42, 1, '2026-05-26 11:44:58', '2026-05-26 11:45:40');
INSERT INTO `practice_record` VALUES (18, 6, '0491c567384144c9ba1ed12538e5196f', 'MISTAKE', 2, NULL, NULL, NULL, 10, 0, 0.00, 0.00, 0, 0, '2026-05-26 11:45:56', NULL);
INSERT INTO `practice_record` VALUES (19, 6, 'ff917cfdbb534de7b4f34bbbf83ae4d4', 'MISTAKE', 2, NULL, NULL, NULL, 10, 3, 30.00, 30.00, 50, 1, '2026-05-26 17:04:36', '2026-05-26 17:05:26');
INSERT INTO `practice_record` VALUES (20, 6, 'c009052097144ea1a44d65efb76297ce', 'TYPE', 2, NULL, 'SINGLE_CHOICE', NULL, 5, 2, 40.00, 20.00, 15, 1, '2026-05-26 17:05:31', '2026-05-26 17:05:46');
INSERT INTO `practice_record` VALUES (21, 6, 'f4f7af8864a24394bd19d938f70b1e99', 'TYPE', 2, NULL, 'SINGLE_CHOICE', NULL, 3, 0, 0.00, 0.00, 8, 1, '2026-05-26 17:06:00', '2026-05-26 17:06:09');
INSERT INTO `practice_record` VALUES (22, 6, '6e63dd632a2645d19bf66f076b484437', 'TYPE', 2, NULL, 'SINGLE_CHOICE', NULL, 3, 2, 66.67, 20.00, 9, 1, '2026-05-26 17:06:12', '2026-05-26 17:06:22');
INSERT INTO `practice_record` VALUES (23, 6, 'b3e5bd1832354e1eac85e778c924c2ec', 'TYPE', 2, NULL, 'SINGLE_CHOICE', NULL, 1, 1, 100.00, 10.00, 3, 1, '2026-05-26 17:06:30', '2026-05-26 17:06:33');
INSERT INTO `practice_record` VALUES (26, 6, '2d3d4f92eb0c48c681740bbe82b43276', 'TYPE', 2, NULL, 'ESSAY', NULL, 8, 5, 62.50, 50.00, 46, 1, '2026-05-27 08:54:16', '2026-05-27 08:55:03');
INSERT INTO `practice_record` VALUES (27, 6, 'f5dea3adf0f94d3cb5291130afb27940', 'TYPE', 6, NULL, 'SINGLE_CHOICE', NULL, 6, 1, 16.67, 10.00, 18, 1, '2026-05-27 08:57:02', '2026-05-27 08:57:20');
INSERT INTO `practice_record` VALUES (28, 6, 'e47a051929974544ada27011ae6bfc47', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 3, 100.00, 30.00, 16, 1, '2026-05-27 08:57:31', '2026-05-27 08:57:47');
INSERT INTO `practice_record` VALUES (29, 6, '2bcb3b8af016464f9eb08b34a824ae2f', 'TYPE', 2, NULL, 'MULTI_CHOICE', NULL, 4, 0, 0.00, 0.00, 7, 1, '2026-05-27 09:03:02', '2026-05-27 09:03:09');
INSERT INTO `practice_record` VALUES (30, 6, 'eb8002c63dbd4fe799d368a15d233f9f', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 3, 100.00, 30.00, 11, 1, '2026-05-27 09:18:31', '2026-05-27 09:18:42');
INSERT INTO `practice_record` VALUES (31, 6, 'e5934f05fc41445e9c2d50548669db64', 'TYPE', 6, NULL, 'SINGLE_CHOICE', NULL, 7, 1, 14.29, 10.00, 19, 1, '2026-05-27 09:18:56', '2026-05-27 09:19:15');
INSERT INTO `practice_record` VALUES (32, 6, '4d2e98327c2e45268f8f1b38638cec60', 'TYPE', 2, NULL, 'SINGLE_CHOICE', NULL, 7, 1, 14.29, 10.00, 19, 1, '2026-05-27 09:19:20', '2026-05-27 09:19:39');
INSERT INTO `practice_record` VALUES (33, 6, '512f86ed1622421aba505e67a36d7a6c', 'MISTAKE', 2, NULL, NULL, NULL, 10, 4, 40.00, 40.00, 21, 1, '2026-05-27 09:20:03', '2026-05-27 09:20:24');
INSERT INTO `practice_record` VALUES (34, 6, '89a82781eb47456380c51861f5b65ec8', 'TYPE', 2, NULL, 'FILL_BLANK', NULL, 3, 0, 0.00, 0.00, 7, 1, '2026-05-27 11:27:21', '2026-05-27 11:27:28');
INSERT INTO `practice_record` VALUES (35, 6, '042dd75d014545e6b77ae63177b515da', 'TYPE', 2, NULL, 'ESSAY', NULL, 3, 3, 100.00, 30.00, 6, 1, '2026-05-27 11:27:49', '2026-05-27 11:27:56');
INSERT INTO `practice_record` VALUES (36, 6, 'f7adee3d3f6d479d92f918c028d6605a', 'TYPE', 2, NULL, 'ESSAY', NULL, 3, 1, 33.33, 10.00, 16, 1, '2026-05-27 11:31:15', '2026-05-27 11:31:31');
INSERT INTO `practice_record` VALUES (37, 6, '13112a604a9b46bfa3237b1ffa7c8eb6', 'TYPE', 2, NULL, 'ESSAY', NULL, 3, 2, 66.67, 20.00, 11, 1, '2026-05-27 11:31:36', '2026-05-27 11:31:48');
INSERT INTO `practice_record` VALUES (38, 6, '6862166cde77444c98b40120c6437ca8', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 0, 0.00, 0.00, 20, 1, '2026-05-27 14:18:13', '2026-05-27 14:18:33');
INSERT INTO `practice_record` VALUES (39, 6, '65e5631da593464b95185b4ef1f9c308', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 0, 0.00, 0.00, 14, 1, '2026-05-27 14:18:38', '2026-05-27 14:18:53');
INSERT INTO `practice_record` VALUES (40, 6, 'b429ae72e50f4fddba8db87fed18a163', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 1, 33.33, 10.00, 40, 1, '2026-05-27 14:18:57', '2026-05-27 14:19:37');
INSERT INTO `practice_record` VALUES (41, 6, '54a3f40c609d433fa3821fa576db2c49', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 0, 0.00, 0.00, 0, 0, '2026-05-27 14:22:01', NULL);
INSERT INTO `practice_record` VALUES (42, 6, 'cf6ddb470437407782dfc6938c7e36dc', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 0, 0.00, 0.00, 12, 1, '2026-05-27 14:22:29', '2026-05-27 14:22:42');
INSERT INTO `practice_record` VALUES (43, 6, 'a175b08a43834d168f6cc919097ae5a4', 'TYPE', 6, NULL, 'ESSAY', NULL, 3, 0, 0.00, 0.00, 101, 1, '2026-05-27 14:22:44', '2026-05-27 14:24:26');
INSERT INTO `practice_record` VALUES (44, 6, '324eb2de0900419194e3a6c6be35c0a7', 'TYPE', 6, NULL, 'SINGLE_CHOICE', NULL, 7, 0, 0.00, 0.00, 0, 0, '2026-05-27 15:24:58', NULL);
INSERT INTO `practice_record` VALUES (45, 6, '77c393e56d4349eca5f04dbf76fd1823', 'TYPE', 2, NULL, 'SINGLE_CHOICE', NULL, 7, 0, 0.00, 0.00, 9, 1, '2026-05-28 08:56:09', '2026-05-28 08:56:18');
INSERT INTO `practice_record` VALUES (46, 6, '7b66dba0440d4a1fa910bb8541398653', 'TYPE', 5, NULL, 'SINGLE_CHOICE', NULL, 7, 0, 0.00, 0.00, 16, 1, '2026-05-28 09:28:08', '2026-05-28 09:28:25');
INSERT INTO `practice_record` VALUES (47, 6, '2fd0a7af7e5047b7a4f4b06a3bd2b728', 'TYPE', 4, NULL, 'SINGLE_CHOICE', NULL, 7, 1, 14.29, 10.00, 17, 1, '2026-05-28 09:55:59', '2026-05-28 09:56:16');

-- ----------------------------
-- Table structure for question
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` bigint NOT NULL,
  `knowledge_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'JSON array of knowledge point IDs',
  `type` enum('SINGLE_CHOICE','MULTI_CHOICE','TRUE_FALSE','FILL_BLANK','SHORT_ANSWER','ESSAY') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'JSON array for choices',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `difficulty` tinyint NULL DEFAULT 3 COMMENT '1-5, 1=easy, 5=hard',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_subject`(`subject_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_difficulty`(`difficulty` ASC) USING BTREE,
  CONSTRAINT `question_ibfk_1` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of question
-- ----------------------------
INSERT INTO `question` VALUES (12, 2, '77,76', 'SINGLE_CHOICE', '【2025新高考一卷·第1题】已知集合A={x|-1<x<2}，B={x|x²-4x+3<0}，则A∩B=', '[\"(1,2)\",\"(-1,1)\",\"(1,3)\",\"(-1,3)\"]', 'A', 'B={x|1<x<3}，A∩B=(1,2)。', 1, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (13, 2, '78', 'SINGLE_CHOICE', '【2025新高考一卷·第2题】已知z=1+i，则z²+2z=', '[\"2i\",\"-2i\",\"2\",\"-2\"]', 'A', 'z²+2z=(1+i)²+2(1+i)=2i+2+2i=2i。', 2, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (14, 2, '75', 'SINGLE_CHOICE', '【2025新高考一卷·第3题】已知向量a=(1,2)，b=(x,4)，若a∥b，则x=', '[\"1\",\"2\",\"3\",\"4\"]', 'B', 'a∥b则1×4=2×x，解得x=2。', 1, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (15, 2, '17', 'SINGLE_CHOICE', '【2025新高考一卷·第4题】某圆柱的高为2，底面半径为1，则该圆柱的侧面积为', '[\"2π\",\"4π\",\"6π\",\"8π\"]', 'B', '圆柱侧面积S=2πrh=2π×1×2=4π。', 1, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (16, 2, '15', 'SINGLE_CHOICE', '【2025新高考一卷·第5题】已知sin(α+π/6)=1/3，则cos(2α-π/3)=', '[\"-7/9\",\"-5/9\",\"5/9\",\"7/9\"]', 'A', '令β=α+π/6，则cos(2α-π/3)=cos(2β-2π/3)。由sinβ=1/3得cosβ=2√2/3，cos2β=1-2sin²β=7/9，代入计算得-7/9。', 4, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (17, 2, '14', 'SINGLE_CHOICE', '【2025新高考一卷·第6题】已知函数f(x)=x³+ax²+bx+c，若f(x)在x=1处取得极值，且f(1)=0，则f(-1)=', '[\"-2\",\"-1\",\"1\",\"2\"]', 'A', '由极值条件和f(1)=0可确定参数关系，计算得f(-1)=-2。', 4, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (18, 2, '18', 'SINGLE_CHOICE', '【2025新高考一卷·第7题】已知双曲线C:x²/a²-y²/b²=1(a>0,b>0)的离心率为2，则C的渐近线方程为', '[\"y=±√3x\",\"y=±x/√3\",\"y=±√2x\",\"y=±x/√2\"]', 'A', 'e=c/a=2，则c=2a，b²=c²-a²=3a²，b=√3a，渐近线为y=±(b/a)x=±√3x。', 3, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (19, 2, '74', 'SINGLE_CHOICE', '【2025新高考一卷·第8题】已知随机变量X~B(4,p)，若P(X≥1)=15/16，则p=', '[\"1/4\",\"1/2\",\"3/4\",\"1/3\"]', 'B', 'P(X≥1)=1-P(X=0)=1-(1-p)⁴=15/16，则(1-p)⁴=1/16，1-p=1/2，p=1/2。', 2, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (20, 2, '16', 'MULTI_CHOICE', '【2025新高考一卷·第9题】已知数列{an}的前n项和为Sn，则下列结论正确的是', '[\"若Sn=n²，则{an}是等差数列\",\"若Sn=2ⁿ-1，则{an}是等比数列\",\"若{an}是等差数列，则S₂n=2n(a₁+an)\",\"若{an}是等比数列且公比q≠1，则Sn=a₁(1-qⁿ)/(1-q)\"]', 'ABD', 'A:an=2n-1等差；B:an=2ⁿ⁻¹等比；C:需要验证；D正确。', 3, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (21, 2, '17', 'MULTI_CHOICE', '【2025新高考一卷·第10题】已知正方体ABCD-A1B1C1D1的棱长为2，则下列结论正确的是', '[\"直线A1C与平面ABCD所成角的正切值为√2\",\"直线A1B与平面AB1C1所成角的正弦值为√3/3\",\"点A到平面A1BD的距离为2√3/3\",\"直线BD与直线A1C1所成角的余弦值为1/2\"]', 'ACD', '通过空间几何计算验证各选项。', 4, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (22, 2, '14', 'MULTI_CHOICE', '【2025新高考一卷·第11题】已知函数f(x)=eˣ(x²-ax+a)，则下列结论正确的是', '[\"当a=3时，f(x)有两个极值点\",\"当a=2时，f(x)在R上单调递增\",\"当a=1时，f(x)的最小值为-1\",\"当a=0时，f(x)在(0,+∞)上单调递增\"]', 'BC', 'f\'(x)=eˣ(x²+2x-a+2)，分析各选项得BC正确。', 4, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (23, 2, '18', 'MULTI_CHOICE', '【2025新高考一卷·第12题】已知抛物线C:y²=4x的焦点为F，点P在C上，则下列结论正确的是', '[\"若|PF|=4，则P的横坐标为3\",\"以PF为直径的圆与y轴相切\",\"|PF|的最小值为1\",\"若直线PF的斜率为k，则|k|≤1\"]', 'ABC', 'A:由定义x+1=4，x=3正确；B:圆心到y轴距离等于半径正确；C:最小值为焦点到顶点距离1正确。', 3, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (24, 2, '75', 'FILL_BLANK', '【2025新高考一卷·第13题】已知平面向量a=(1,2)，b=(2,x)，若|a+b|=|a-b|，则x=', NULL, '-1', '由|a+b|²=|a-b|²得a·b=0，即1×2+2×x=0，x=-1。', 2, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (25, 2, '14,76', 'FILL_BLANK', '【2025新高考一卷·第14题】已知函数f(x)=ln(x²+1)-ax在(0,+∞)上单调递减，则a的取值范围是______。', NULL, '[1,+∞)', 'f\'(x)=2x/(x²+1)-a≤0对x>0恒成立，则a≥2x/(x²+1)的最大值1。', 4, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 10:57:40');
INSERT INTO `question` VALUES (26, 2, '17', 'FILL_BLANK', '【2025新高考一卷·第15题】已知圆锥的底面半径为1，母线长为2，则该圆锥的内切球半径为______。', NULL, '√3/3', '圆锥高h=√(4-1)=√3，由相似三角形得r/(√3-r)=1/2，解得r=√3/3。', 3, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (27, 2, '18', 'FILL_BLANK', '【2025新高考一卷·第16题】已知椭圆C:x²/a²+y²/b²=1(a>b>0)的左、右焦点分别为F1、F2，点P在C上，若|PF1|=2|PF2|，且∠F1PF2=π/2，则C的离心率为______。', NULL, '√2/2', '设|PF2|=m，则|PF1|=2m，由椭圆定义3m=2a。由勾股定理得离心率。', 5, '2025新高考一卷', NULL, '2026-05-20 09:50:17', '2026-05-26 09:02:04');
INSERT INTO `question` VALUES (44, 2, '15', 'ESSAY', '【2025新高考一卷·第17题】(本小题满分10分)\n已知函数f(x)=sinx+cosx+2sinxcosx。\n(1)求f(x)的最小正周期；\n(2)求f(x)在[0,π/2]上的值域。', NULL, '(1)f(x)=√2sin(x+π/4)+sin2x，最小正周期为2π。\n(2)在[0,π/2]上，f(x)的值域为[1,1+√2]。', '先化简三角表达式，再结合区间讨论函数值域。', 3, '2025新高考一卷', NULL, '2026-05-26 10:57:40', '2026-05-26 10:57:40');
INSERT INTO `question` VALUES (45, 2, '16', 'ESSAY', '【2025新高考一卷·第18题】(本小题满分12分)\n已知数列{an}满足a1=1，an+1=2an+1。\n(1)证明：{an+1}是等比数列；\n(2)设bn=nan，求数列{bn}的前n项和Tn。', NULL, '(1)an+1+1=2(an+1)，所以{an+1}是首项为2，公比为2的等比数列。\n(2)an=2ⁿ-1，bn=n(2ⁿ-1)，Tn需要错位相减法计算。', '关键是构造等比数列，并使用错位相减处理n·2ⁿ型求和。', 3, '2025新高考一卷', NULL, '2026-05-26 10:57:40', '2026-05-26 10:57:40');
INSERT INTO `question` VALUES (46, 2, '17', 'ESSAY', '【2025新高考一卷·第19题】(本小题满分12分)\n如图，四棱锥P-ABCD中，底面ABCD是直角梯形，∠ABC=90°，AB=BC=2，CD=1，PA⊥平面ABCD，PA=2。\n(1)证明：BD⊥平面PAC；\n(2)求二面角P-CD-A的正弦值。', NULL, '(1)建立空间直角坐标系证明BD⊥AC，BD⊥PA。\n(2)求出平面PCD和平面ACD的法向量，计算二面角的正弦值。', '用线面垂直判定和空间向量法处理二面角。', 4, '2025新高考一卷', NULL, '2026-05-26 10:57:40', '2026-05-26 10:57:40');
INSERT INTO `question` VALUES (47, 2, '14', 'ESSAY', '【2025新高考一卷·第20题】(本小题满分12分)\n已知函数f(x)=eˣ-ax-a。\n(1)讨论f(x)的单调性；\n(2)若f(x)有两个零点，求a的取值范围。', NULL, '(1)根据导数和参数a讨论单调区间。\n(2)结合极值与端点趋势，得到f(x)有两个零点的参数范围。', '导数与零点问题，重点是分类讨论参数和极值条件。', 4, '2025新高考一卷', NULL, '2026-05-26 10:57:40', '2026-05-26 10:57:40');
INSERT INTO `question` VALUES (48, 2, '18', 'ESSAY', '【2025新高考一卷·第21题】(本小题满分12分)\n已知椭圆C:x²/a²+y²/b²=1(a>b>0)的离心率为√2/2，且经过点(1,√2/2)。\n(1)求C的方程；\n(2)设直线l与C交于A、B两点，O为坐标原点，若OA⊥OB，证明：直线l过定点。', NULL, '(1)e=c/a=√2/2，代入点(1,√2/2)得a²=2，b²=1，C的方程为x²/2+y²=1。\n(2)设直线l:y=kx+m，由OA⊥OB推导证明直线过定点。', '先求椭圆标准方程，再用韦达定理和垂直条件推定点。', 4, '2025新高考一卷', NULL, '2026-05-26 10:57:40', '2026-05-26 10:57:40');
INSERT INTO `question` VALUES (49, 2, '76', 'ESSAY', '【2025新高考一卷·第22题】(本小题满分12分)\n某工厂生产甲、乙两种产品，每种产品都需要经过A、B两道工序。生产一件甲产品需要A工序2小时、B工序1小时；生产一件乙产品需要A工序1小时、B工序2小时。A工序每天最多工作10小时，B工序每天最多工作8小时。生产一件甲产品获利300元，生产一件乙产品获利200元。\n(1)问每天应生产甲、乙产品各多少件，才能使利润最大？\n(2)若A工序每天最多工作的小时数增加到12小时，其他条件不变，求最大利润。', NULL, '(1)设生产甲x件，乙y件，利润z=300x+200y。约束条件：2x+y≤10，x+2y≤8，x≥0，y≥0。解得x=4，y=2时利润最大为1600元。\n(2)A工序增加到12小时后重新计算最优解。', '线性规划应用题，重点是建立约束条件并比较可行域顶点。', 3, '2025新高考一卷', NULL, '2026-05-26 10:57:40', '2026-05-26 10:57:40');
INSERT INTO `question` VALUES (84, 6, '104', 'SINGLE_CHOICE', '【2025高考课标卷生物·第1题】<br>1. 蛋白质是结构和功能多样的生物大分子。下列叙述错误的是（）', '[\"二硫键的断裂不会改变蛋白质的空间结构\", \"改变蛋白质的空间结构可能会影响其功能\", \"用乙醇等有机溶剂处理可使蛋白质发生变性\", \"利用蛋白质工程可获得氨基酸序列改变的蛋白质\"]', 'A', '参考答案：A', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (85, 6, '105', 'SINGLE_CHOICE', '【2025高考课标卷生物·第2题】<br>2. 在一定温度下，生长在大田的某种植物光合速率（CO2固定速率）和呼吸速率（CO2释放速率）对光照强度的响应曲线如图所示。下列叙述错误的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/biology/img_001.png\" alt=\"题目配图\"></div>', '[\"光照强度为a时，该植物的干重不会增加\", \"光照强度从a逐渐增加到b时，该植物生长速率逐渐增大\", \"光照强度小于b时，提高大田CO2浓度，CO2固定速率会增大\", \"光照强度为b时，适当降低光反应速率，CO2固定速率会降低\"]', 'C', '参考答案：C', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (86, 6, '106', 'SINGLE_CHOICE', '【2025高考课标卷生物·第3题】<br>3. 为研究肾上腺的生理机能，某研究小组将小鼠按照下表进行处理，一定时间后检测相关指标。<br>分组<br>实验处理<br>甲<br>不摘除肾上腺<br>乙<br>摘除肾上腺<br>丙<br>摘除肾上腺，注射醛固酮<br>下列叙述错误的是（）', '[\"乙组小鼠的促肾上腺皮质激素水平会升高\", \"乙组小鼠饮生理盐水有利于改善水盐平衡\", \"三组小鼠均饮清水时，丙组小鼠血钠含量最低\", \"甲组小鼠受寒冷刺激时，肾上腺素释放量增加\"]', 'C', '参考答案：C', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (87, 6, '108', 'SINGLE_CHOICE', '【2025高考课标卷生物·第4题】<br>4. 某同学在甲、乙两个植物群落中设置样方调查其特征，样方中植物的物种数随样方面积扩大而逐渐增加，但样方面积扩大到一定程度后物种数的变化明显趋缓（如图所示），此时对应的样方面积（a和b）通常称为最小面积。下列叙述错误的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/biology/img_002.png\" alt=\"题目配图\"></div>', '[\"最小面积样方中应包含群落中绝大多数的物种\", \"与甲相比，乙群落的物种丰富度较高，调查时最小面积更大\", \"调查甲群落的物种丰富度时，设置的样方面积应不小于a\", \"调查乙群落中植物的种群密度时，针对每种植物设置的样方面积应不小于b\"]', 'D', '参考答案：D', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (88, 6, '107', 'SINGLE_CHOICE', '【2025高考课标卷生物·第5题】<br>5. 为获得作物新品种，可采用不同的育种技术。下列叙述错误的是（）', '[\"三倍体西瓜育种时，利用了人工诱导染色体加倍获得的多倍体\", \"作物单倍体育种时，利用了由植物茎尖组织培养获得的单倍体\", \"航天育种时，利用了太空多种因素导致基因突变产生的突变体\", \"水稻杂交育种时，利用了水稻有性繁殖过程中产生的重组个体\"]', 'B', '参考答案：B', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (89, 6, '110', 'SINGLE_CHOICE', '【2025高考课标卷生物·第6题】<br>6. 琼脂糖凝胶电泳常用于核酸样品的分析，样品1～4的电泳结果如图所示（“+”“-”分别代表电泳槽的阳极和阴极）。已知样品1和2中的DNA分子分别是甲和乙，甲只有限制酶R的一个酶切位点，样品3和4中有一个样品是甲的酶切产物。下列叙述错误的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/biology/img_003.png\" alt=\"题目配图\"></div>', '[\"配制琼脂糖凝胶时需选用适当 缓冲溶液\", \"该实验条件下甲、乙两种DNA分子均带负电荷\", \"甲、乙两种DNA分子所含碱基对的数量可能不同\", \"据图推测样品3可能是甲被酶R完全酶切后的产物\"]', 'D', '参考答案：D', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (90, 6, '111', 'ESSAY', '【2025高考课标卷生物·第7题】<br>7. 将某植物叶肉细胞放入一定浓度 KCl溶液中，起初细胞失水发生质壁分离，一定时间（t）后细胞开始吸水，并逐渐复原。回答下列问题：<br>（1）植物细胞与外界溶液进行水分交换时，水分子跨膜运输的两种方式是________。<br>（2）细胞失水发生质壁分离，原生质层与细胞壁分离的原因是________。<br>（3）一定时间（t）后细胞开始吸水的原因是________。', NULL, '（1）自由扩散、协助扩散<br>（2）细胞失水体积缩小，原生质层比细胞壁的伸缩性大<br>（3）K+和C1-进入细胞，使细胞内渗透压高于外界溶液', '参考答案：<br>（1）自由扩散、协助扩散<br>（2）细胞失水体积缩小，原生质层比细胞壁的伸缩性大<br>（3）K+和C1-进入细胞，使细胞内渗透压高于外界溶液', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (91, 6, '109', 'ESSAY', '【2025高考课标卷生物·第8题】<br>8. 有研究显示，机体内蛋白P表达量降低会引起免疫失调。已知酶E可催化蛋白P基因的启动子甲基化，酶E被磷酸化后失活。研究人员用酶E（或磷酸化的酶E）、含蛋白P基因及其启动子的表达质粒等进行实验，结果如图所示。回答下列问题：<br>（1）免疫失调包括过敏反应和________（答出2点即可）等。<br>（2）根据实验结果判断，蛋白P基因的启动子甲基化________（填“促进”“抑制”或“不影响”）蛋白P的表达，判断依据是________。<br>（3）为治疗因蛋白P表达量降低起的免疫失调，可使用抑制________（填“酶E”“磷酸化的酶E”或“蛋白P”）活性的药物。免疫失调也可以通过调节抗体的生成进行治疗，机体产生抗体过程中记忆B细胞的作用是________。<div class=\"question-images\"><img src=\"/images/questions/2025/biology/img_004.png\" alt=\"题目配图\"></div>', NULL, '（1）自身免疫病、免疫缺陷病<br>（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量<br>（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', '参考答案：<br>（1）自身免疫病、免疫缺陷病<br>（2） ①. 抑制 ②. 酶E催化蛋白P基因的启动子甲基化，酶E含量增加导致蛋白P的表达量下降，磷酸化的酶E含量增加不会影响蛋白P的表达量<br>（3） ①. 酶E ②. 再次接触抗原时，能迅速增殖分化，快速产生大量抗体', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (92, 6, '108', 'ESSAY', '【2025高考课标卷生物·第9题】<br>9. 在“绿水青山就是金山银山”理念的感召下，同学们积极讨论某退化荒山的生态恢复方案。A同学提出选择一种树种进行全覆盖造林；B同学提出应该种植多种草本和木本植物。回答下列问题：<br>（1）在生态恢复过程中，退化荒山会发生群落演替。通常，群落演替的类型有初生演替和次生演替，二者的区别有________（答出2点即可）。<br>（2）与A同学的方案相比，B同学的方案可能有利于控制害虫的爆发，从种间关系的角度分析其原因是________。<br>（3）为合理利用环境资源，从群落空间结构的角度考虑，设计荒山绿化方案时应遵循的原则是________（答出2点即可）。<br>（4）为维护恢复后生态系统的稳定性，需要采取的措施有________（答出2点即可）。', NULL, '（1）起点不同；速度不同<br>（2）B同学方案中植物种类多，动物种类会相应增加，物种间的竞争、捕食等关系更复杂，使害虫种群数量增长受到限制<br>（3）群落水平方向上种植不同种类 植物；群落垂直方向上种植不同高度的植物<br>（4）控制对生态系统的干扰强度；给予相应的物质、能量投入', '参考答案：<br>（1）起点不同；速度不同<br>（2）B同学方案中植物种类多，动物种类会相应增加，物种间的竞争、捕食等关系更复杂，使害虫种群数量增长受到限制<br>（3）群落水平方向上种植不同种类 植物；群落垂直方向上种植不同高度的植物<br>（4）控制对生态系统的干扰强度；给予相应的物质、能量投入', 3, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (93, 6, '107', 'ESSAY', '【2025高考课标卷生物·第10题】<br>10. 植物合成的色素会影响花色。某二倍体植物的花色有深红、浅红和白三种表型。研究小组用甲、乙两个浅红色表型的植株进行相关实验。回答下列问题：<br>（1）甲、乙分别自交，子一代均出现浅红色：白色=3：1的表型分离比；甲和乙杂交，子一代出现深红色（丙）：浅红色：白色（丁）=1：2：1的表型分离比。综上判断，甲和乙的基因型________（填“相同”或“不同”），判断依据是________。<br>（2）丙自交子一代出现深红色：浅红色：白色=9：6：1的表型分离比，其中与丙基因型相同的个体所占比例为________。若丙与丁杂交，子一代的表型及分离比为________，其中纯合体所占比例为________。', NULL, '（1） ①. 不同 ②. 甲、乙自交的结果与甲乙杂交的结果不同<br>（2） ①. 1/4 ②. 深红色:浅红色:白色=1:2:1 ③. 1/4', '参考答案：<br>（1） ①. 不同 ②. 甲、乙自交的结果与甲乙杂交的结果不同<br>（2） ①. 1/4 ②. 深红色:浅红色:白色=1:2:1 ③. 1/4', 4, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (94, 6, '110', 'ESSAY', '【2025高考课标卷生物·第11题】<br>11. 为在大肠杆菌中表达酶X，某同学将编码酶X的基因（目的基因）插入质粒P0，构建重组质粒Px，并转入大肠杆菌。该同学设计引物用PCR方法验证重组质粒构建成功（引物1~4结合位置如图所示，→表示引物5＇→3＇方向）。回答下列问题：<br>（1）PCR是根据DNA复制原理在体外扩增DNA的技术。在细胞中DNA复制时解开双链的酶是________，而PCR过程中解开双链的方法是________。<br>（2）PCR过程中，因参与合成反应、不断消耗而浓度下降的组分有________。<br>（3）该同学进行PCR实验时，所用模板与引物见下表。实验中①和④的作用是：________；②无扩增产物，原因是________；③、⑤和⑥有扩增产物，扩增出的DNA产物分别是________。<br>管号<br>①<br>②<br>③<br>④<br>⑤<br>⑥<br>模板<br>无<br>P0<br>Px<br>无<br>P0<br>Px<br>引物对<br>引物1和引物2<br>引物3和引物4<br>（4）设计实验验证大肠杆菌表达 酶X有活性，简要写出实验思路和预期结果________。<div class=\"question-images\"><img src=\"/images/questions/2025/biology/img_005.png\" alt=\"题目配图\"></div>', NULL, '（1） ①. 解旋酶 ②. 高温变性<br>（2）引物、脱氧核苷三磷酸<br>（3） ①. 作为对照（或答:鉴定反应体系是否有模板污染） ②. P0不含与引物1和引物2互补的碱基序列 ③. 目的基因、质粒片段、含目的基因和部分质粒序列的片段<br>（4）提取酶X，催化相应底物（反应物）的反应，检测是否有产物生成。有产物生成，则证明酶X有活性', '参考答案：<br>（1） ①. 解旋酶 ②. 高温变性<br>（2）引物、脱氧核苷三磷酸<br>（3） ①. 作为对照（或答:鉴定反应体系是否有模板污染） ②. P0不含与引物1和引物2互补的碱基序列 ③. 目的基因、质粒片段、含目的基因和部分质粒序列的片段<br>（4）提取酶X，催化相应底物（反应物）的反应，检测是否有产物生成。有产物生成，则证明酶X有活性', 4, '2025高考课标卷生物', 1, '2026-05-27 08:53:56', '2026-05-27 08:53:56');
INSERT INTO `question` VALUES (106, 5, '112', 'SINGLE_CHOICE', '【2025全国卷理综化学·第7题】<br>7．化学与人类生活密切相关。下列叙述正确的是（）', '[\"硫酸铜具有杀菌作用，可用作饮用水消毒剂\", \"小苏打遇酸能产生气体，可用作食品膨松剂\", \"碳化硅抗氧化且耐高温，可用作固体电解质\", \"聚氯乙烯塑料制品耐腐蚀，可用作食品包装\"]', 'B', '参考答案：B', 3, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (107, 5, '113', 'SINGLE_CHOICE', '【2025全国卷理综化学·第8题】<br>8．下列关于铁腐蚀与防护的反应式正确的是（）', '[\"酸性环境中铁发生析氢腐蚀的负极反应： （公式见原题）\", \"铁发生腐蚀生锈的反应： （公式见原题）\", \"铁经过发蓝处理形成致密氧化膜： （公式见原题）\", \"安装锌块保护船舶外壳，铁电极上发生的反应： （公式见原题）\"]', 'A', '参考答案：A', 3, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (108, 5, '114', 'SINGLE_CHOICE', '【2025全国卷理综化学·第9题】<br>9．我国研究人员利用手性催化剂 （公式见原题） 合成了具有优良生物相容性的 （公式见原题） 。下列叙述正确的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_001.png\" alt=\"题目配图\"></div>', '[\"该反应为缩聚反应\", \"（公式见原题） 分子中所有碳原子共平面\", \"聚合反应过程中，b键发生断裂\", \"（公式见原题） 碱性水解生成单一化合物\"]', 'C', '参考答案：C', 3, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (109, 5, '113', 'SINGLE_CHOICE', '【2025全国卷理综化学·第10题】<br>10．某研究小组设计如下电解池，既可将中性废水中的硝酸盐转化为氨，又可将废塑料（PET）碱性水解液中的乙二醇转化为羟基乙酸盐，实现变废为宝。<br>电解时，下列说法错误的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_002.png\" alt=\"题目配图\"></div>', '[\"阳极区 （公式见原题） 下降\", \"（公式见原题） 从阴极区向阳极区迁移\", \"阴极发生反应 （公式见原题）\", \"阴极转化 （公式见原题） ，阳极将生成 （公式见原题）\"]', 'D', '参考答案：D', 3, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (110, 5, '115', 'SINGLE_CHOICE', '【2025全国卷理综化学·第11题】<br>11．一种化合物分子结构如图所示，其中W、X、Y、Z为短周期元素，原子序数依次增大，W的种同位素的中子数为0，X和Z同族。下列说法错误的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_003.png\" alt=\"题目配图\"></div>', '[\"原子半径： （公式见原题）\", \"第一电离能： （公式见原题）\", \"电负性： （公式见原题）\", \"单质氧化性： （公式见原题）\"]', 'A', '参考答案：A', 4, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (111, 5, '116', 'SINGLE_CHOICE', '【2025全国卷理综化学·第12题】<br>12．研究发现水微滴表面有强电场，能引发反应 （公式见原题） 。三唑水溶液微滴表面接触 （公式见原题） 发生反应，可能的反应机理如图所示。<br>根据上述反应机理，下列叙述错误的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_004.png\" alt=\"题目配图\"></div>', '[\"三唑在反应循环中起催化作用\", \"（公式见原题） 换成 （公式见原题） ，可生成 （公式见原题）\", \"碳原子轨道的杂化存在从 （公式见原题） 到 （公式见原题） 的转变\", \"总反应为 （公式见原题）\"]', 'B', '参考答案：B', 4, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (112, 5, '116', 'SINGLE_CHOICE', '【2025全国卷理综化学·第13题】<br>13． （公式见原题） 总浓度 （公式见原题） 为 （公式见原题） 的水溶液中存在平衡： （公式见原题） 、 （公式见原题） 。溶液中 （公式见原题） 、 （公式见原题） 、 （公式见原题） 与 （公式见原题） 关系如下图所示。<br>下列叙述正确的是（）<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_005.png\" alt=\"题目配图\"></div>', '[\"M、N分别为 （公式见原题） 、 （公式见原题） 关系曲线\", \"（公式见原题）\", \"溶液中 （公式见原题）\", \"（公式见原题） 的溶液中 （公式见原题）\"]', 'B', '参考答案：B', 4, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (113, 5, '117', 'ESSAY', '【2025全国卷理综化学·第26题】<br>26．（14分）碘具有重要经济价值。实验室利用沉淀法从含碘废液中回收碘，相关反应的化学方程式为：<br>（公式见原题）<br>（公式见原题）<br>实验装置如图所示。<br>实验步骤如下：<br>①向A中加入 （公式见原题） ，搅拌使其溶解。将 （公式见原题） 饱和溶液加入B中。<br>②加热至 （公式见原题） ，逐滴加入饱和 （公式见原题） 溶液。停止加热，静置，沉降。<br>③检查 （公式见原题） 是否沉淀完全。确认沉淀完全后，弃去上层清液。<br>④将B中溶液更换为浓硝酸，连接装置D。不断搅拌下，逐滴加入浓硝酸。<br>⑤待析出的 （公式见原题） 沉降后，过滤，洗涤，干燥得到产品。<br>回答下列问题：<br>（1）仪器A和C的名称分别是___________、___________。<br>（2）称取 （公式见原题） 于烧杯中，向其中加入适量蒸馏水，微热，搅拌，静置冷却，得到 （公式见原题） 饱和溶液。判断 （公式见原题） 溶液饱和的实验现象是____________________________________。<br>（3）步骤③中，确认 （公式见原题） 沉淀完全的操作及现象是：取少量清液，加入一滴淀粉溶液，________。<br>（4）步骤④中，加入浓硝酸后A中的现象是________________，D中盛放________。<br>（5）步骤⑤中，使用________（填标号）洗涤。<br>a．水b．四氟化碳c．乙醇<br>（6）若要进一步精制产品，可采取的方法是________。<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_006.png\" alt=\"题目配图\"></div>', NULL, '（1）三颈瓶温度计<br>（2）烧杯底部有少量固体<br>（3）缓慢滴加过量氯水，无蓝色出现<br>（4）白色固体消失，析出紫黑色沉淀，产生红棕色气体，溶液呈绿色碱液<br>（5）a<br>（6）升华', '参考答案：<br>（1）三颈瓶温度计<br>（2）烧杯底部有少量固体<br>（3）缓慢滴加过量氯水，无蓝色出现<br>（4）白色固体消失，析出紫黑色沉淀，产生红棕色气体，溶液呈绿色碱液<br>（5）a<br>（6）升华', 4, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (114, 5, '118', 'ESSAY', '【2025全国卷理综化学·第27题】<br>27．（14分）我国的蛇纹石资源十分丰富，它的主要成分是 （公式见原题） ，伴生有少量 （公式见原题） 、 （公式见原题） 、 （公式见原题） 等元素。利用蛇纹石转化与绿矾分解的耦合回收 （公式见原题） 并矿化固定二氧化碳的实验流程如图所示。<br>已知：<br>（公式见原题）<br>（公式见原题）<br>（公式见原题）<br>（公式见原题）<br>（公式见原题）<br>（公式见原题）<br>（公式见原题）<br>（公式见原题）<br>（公式见原题）<br>回答下列问题：<br>（1）绿矾（ （公式见原题） ）在高温下分解，得到红棕色固体和气体产物，反应的化学方程式为________。<br>（2）经“焙烧①”“水浸”，过滤分离后，滤液中金属离子的浓度（ （公式见原题） ）分别为： （公式见原题） 、 （公式见原题） 、 （公式见原题） 、 （公式见原题） 。滤渣①的主要成分是________、________。<br>（3）加入 （公式见原题） “调 （公式见原题） ”，过滤后，滤渣②是________、________，滤液中 （公式见原题） 的浓度为________ （公式见原题） 。<br>（4）“焙烧②”后得到 （公式见原题） 。 （公式见原题） 晶胞如图所示，晶胞中含有 （公式见原题） 的个数为________。<br>（5）调节“沉镍”后的溶液为碱性，“矿化”反应的离子方程式为________________。<br>（6） （公式见原题） 蛇纹石“矿化”固定 （公式见原题） ，得到 （公式见原题） ，相当于固定 （公式见原题） ________L（标准状况）。<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_007.png\" alt=\"题目配图\"><img src=\"/images/questions/2025/chemistry/img_008.png\" alt=\"题目配图\"></div>', NULL, '（1） （公式见原题）<br>（2） （公式见原题） （公式见原题） （3） （公式见原题） （公式见原题） （公式见原题） （4）4<br>（5） （公式见原题） （6）89.6', '参考答案：<br>（1） （公式见原题）<br>（2） （公式见原题） （公式见原题） （3） （公式见原题） （公式见原题） （公式见原题） （4）4<br>（5） （公式见原题） （6）89.6', 4, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (115, 5, '116', 'ESSAY', '【2025全国卷理综化学·第28题】<br>28．（15分）乙酸乙酯是一种应用广泛的有机化学品，可由乙酸和乙醇通过酯化反应制备。回答下列问题：<br>（1）乙酸、乙醇和乙酸乙酯的燃烧热分别为 （公式见原题） 、 （公式见原题） 和 （公式见原题） ，则酯化反应 （公式见原题） 的 （公式见原题） ____________ （公式见原题） 。<br>（2）酯化反应中的3种有机物的佛点从高到低的顺序为________原因是______________________________。<br>（3）在常压和 （公式见原题） 时，初始组成 （公式见原题） 、 （公式见原题） 作催化剂的条件下进行反应，得到乙醇浓度随反应时间的变化如下图所示。<br>平衡时乙酸的转化率 （公式见原题） ________ （公式见原题） ，平衡常数 （公式见原题） ________（保留2位有效数字）。已知酯化反应的速率方程为 （公式见原题） ，其中 （公式见原题） ，则 （公式见原题） _________ （公式见原题） （保留2位有效数字）。<br>（4）研究发现，难以通过改变反应温度或压强来提高乙酸乙酯平衡产率，原因是________________。若要提高乙酸乙酯的产率，可以采用的方法是________________（举1例）。<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_009.png\" alt=\"题目配图\"></div>', NULL, '（1） （公式见原题）<br>（2） （公式见原题）<br>乙酸乙酯分子间不存在氢键，乙酸分子间的氢键比乙醇的更强<br>（3）303.9 （公式见原题）<br>（4） （公式见原题） 很小，温度对平衡影响小；液相反应，压强对平衡影响小及时移出产物', '参考答案：<br>（1） （公式见原题）<br>（2） （公式见原题）<br>乙酸乙酯分子间不存在氢键，乙酸分子间的氢键比乙醇的更强<br>（3）303.9 （公式见原题）<br>（4） （公式见原题） 很小，温度对平衡影响小；液相反应，压强对平衡影响小及时移出产物', 4, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (116, 5, '114', 'ESSAY', '【2025全国卷理综化学·第29题】<br>29．（15分）艾拉莫德（化合物F）有抗炎镇痛作用，可用于治疗类风湿关节炎。F的一条合成路线如下（略去部分条件和试剂）<br>回答下列问题：<br>（1）A的结构简式是________。<br>（2）B中官能团的名称是________、________。<br>（3）反应②的反应类型为________；吡啶是一种有机碱，在反应②中除了作溶剂外，还起到的作用是________________。<br>（4）在反应⑤的步骤中，二甲基丙酰氯和甲酸钠预先在溶剂丙酮中混合搅拌5小时，写出此过程的化学方程式________________。然后，再加入D进行反应。<br>（5）关于F的化学性质，下列判断错误的是________（填标号）。<br>a．可发生银镜反应 b．可发生碱性水解反应<br>c．可使 （公式见原题） 溶液显紫色 d．可使酸性 （公式见原题） 溶液褪色<br>（6）G是二甲基丙酰氯的同分异构体，可以发生银镜反应，核磁共振氢谱中显示为四组峰，且峰面积比为 （公式见原题） 。G的结构简式是________________（手性碳用*号标记）。<br>（7）参照反应①和②，利用 和 完成杀菌剂乙霉威（ ）的合成路线：<div class=\"question-images\"><img src=\"/images/questions/2025/chemistry/img_010.png\" alt=\"题目配图\"><img src=\"/images/questions/2025/chemistry/img_011.png\" alt=\"题目配图\"><img src=\"/images/questions/2025/chemistry/img_012.png\" alt=\"题目配图\"><img src=\"/images/questions/2025/chemistry/img_013.png\" alt=\"题目配图\"><img src=\"/images/questions/2025/chemistry/img_014.png\" alt=\"题目配图\"></div>', NULL, '（1）<br>（2）氨基醚键<br>（3）取代反应吸收反应产生的 （公式见原题） ，促进反应进行<br>（4）<br>（5）c<br>（6）<br>（7）', '参考答案：<br>（1）<br>（2）氨基醚键<br>（3）取代反应吸收反应产生的 （公式见原题） ，促进反应进行<br>（4）<br>（5）c<br>（6）<br>（7）', 4, '2025全国卷理综化学', 1, '2026-05-28 09:13:24', '2026-05-28 09:13:24');
INSERT INTO `question` VALUES (117, 4, '119', 'SINGLE_CHOICE', '【2025高考课标卷物理·第1题】<br>1. 我国自主研发的CR450动车组试验时的速度可达450km/h。若以120m/s的初速度在平直轨道上行驶的CR450动车组，匀减速运行14.4km后停止，则减速运动中其加速度的大小为（　　）', '[\"0.1m/s2\", \"0.5m/s2\", \"1.0m/s2\", \"1.5m/s2\"]', 'B', '参考答案：B', 3, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (118, 4, '120', 'SINGLE_CHOICE', '【2025高考课标卷物理·第2题】<br>2. “天都一号”通导技术试验卫星测距试验的成功，标志着我国在深空轨道精密测量领域取得了技术新突破。“天都一号”在环月椭圆轨道上运行时，（　　）', '[\"受月球的引力大小保持不变\", \"相对月球的速度大小保持不变\", \"离月球越近，其相对月球 速度越大\", \"离月球越近，其所受月球的引力越小\"]', 'C', '参考答案：C', 3, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (119, 4, '121', 'SINGLE_CHOICE', '【2025高考课标卷物理·第3题】<br>3. 如图，撑杆跳高运动中，运动员经过助跑、撑杆起跳，最终越过横杆。若运动员起跳前助跑速度为10m/s，则理论上运动员助跑获得 动能可使其重心提升的最大高度为（重力加速度取10m/s2）（　　）<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_001.png\" alt=\"题目配图\"></div>', '[\"4m\", \"5m\", \"6m\", \"7m\"]', 'B', '参考答案：B', 3, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (120, 4, '122', 'SINGLE_CHOICE', '【2025高考课标卷物理·第4题】<br>4. 匀强电场中，一带正电的点电荷仅在电场力的作用下以某一初速度开始运动，则运动过程中，其（　　）', '[\"所处位置的电势一定不断降低\", \"所处位置的电势一定不断升高\", \"轨迹可能是与电场线平行 直线\", \"轨迹可能是与电场线垂直的直线\"]', 'C', '参考答案：C', 3, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (121, 4, '123', 'SINGLE_CHOICE', '【2025高考课标卷物理·第5题】<br>5. 如图，正方形abcd内有方向垂直于纸面的匀强磁场，电子在纸面内从顶点a以速度v0射入磁场，速度方向垂直于ab。磁感应强度的大小不同时，电子可分别从ab边的中点、b点和c点射出，在磁场中运动的时间分别为t1、t2和t3，则（）<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_002.png\" alt=\"题目配图\"></div>', '[\"t1&lt;t2=t3\", \"t1&lt;t2&lt;t3\", \"t1=t2&gt;t3\", \"t1&gt;t2&gt;t3\"]', 'A', '参考答案：A', 3, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (122, 4, '124', 'MULTI_CHOICE', '【2025高考课标卷物理·第6题】<br>6. 如图，一定量的理想气体先后处于 （公式见原题） 图上 （公式见原题） 三个状态，三个状态下气体的压强分别为 （公式见原题） ，则（）<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_003.png\" alt=\"题目配图\"></div>', '[\"（公式见原题）\", \"（公式见原题）\", \"（公式见原题）\", \"（公式见原题）\"]', 'AD', '参考答案：AD', 4, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (123, 4, '125', 'MULTI_CHOICE', '【2025高考课标卷物理·第7题】<br>7. 一组身高相近的学生沿一直线等间隔排成一排，从左边第一位同学开始，依次周期性地“下蹲、起立”，整个队列呈现类似简谐波的波浪效果，如图所示。假定某次游戏中，形成的波形的波长为4m，左边第一位同学蹲至最低点时，队列中另一同学恰好站直，则这两位同学间的距离可能是（）<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_004.png\" alt=\"题目配图\"></div>', '[\"1m\", \"2m\", \"5m\", \"6m\"]', 'BD', '参考答案：BD', 4, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (124, 4, '125', 'MULTI_CHOICE', '【2025高考课标卷物理·第8题】<br>8. 如图，过P点的虚线上方存在方向垂直于纸面的匀强磁场。一金属圆环在纸面内以P点为轴沿顺时针方向匀速转动，O为圆环的圆心，OP为圆环的半径。则（）<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_005.png\" alt=\"题目配图\"></div>', '[\"圆环中感应电流始终绕O逆时针流动\", \"OP与虚线平行时圆环中感应电流最大\", \"圆环中感应电流变化的周期与环转动周期相同\", \"圆环在磁场内且OP与虚线垂直时环中感应电流最大\"]', 'BC', '参考答案：BC', 4, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (125, 4, '126', 'ESSAY', '【2025高考课标卷物理·第9题】<br>9. 某探究小组利用橡皮筋完成下面实验。<br>（1）将粘贴有坐标纸 木板竖直放置。橡皮筋的一端用图钉固定在木板上，另一端悬挂钩码。钩码质量分别为200g、250g、⋯、500g，平衡时橡皮筋底端在坐标纸上对应的位置如图（a）中圆点所示（钩码的质量在图中用数字标出）。悬挂的钩码质量分别为200g和300g时，橡皮筋底端位置间的距离为___________cm。<br>（2）根据图（a）中各点的位置可知，在所测范围内橡皮筋长度的增加量与所挂钩码的__________（选填“质量”或“质量的增加量”）成正比，由此可求出橡皮筋的劲度系数为___________ （公式见原题） （保留2位有效数字，重力加速度取 （公式见原题） ）。<br>（3）悬挂的钩码质量为 （公式见原题） 时，在橡皮筋底端施以水平向右的力 （公式见原题） ，平衡时橡皮筋方向如图（b）中虚线所示，图（b）中测力计的示数给出了力 （公式见原题） 的大小，则 （公式见原题） ___________ （公式见原题） ， （公式见原题） ___________g（选填“200”“300”或“400”）。<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_006.png\" alt=\"题目配图\"></div>', NULL, '（1）1.90<br>（2） ①. 质量的增加量 ②. 52<br>（3） ①. 1.00 ②. 300', '参考答案：<br>（1）1.90<br>（2） ①. 质量的增加量 ②. 52<br>（3） ①. 1.00 ②. 300', 3, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (126, 4, '126', 'ESSAY', '【2025高考课标卷物理·第10题】<br>10. 用伏安法可以研究电学元件的伏安特性。阻值不随电流、电压变化的元件称为线性电阻元件，否则称为非线性电阻元件。<br>（1）利用伏安法测量某元件的电阻，电流表和电压表的示数分别记为 （公式见原题） 和 （公式见原题） 。若将电流表内接，则 （公式见原题） ___________元件两端的电压， （公式见原题） ___________元件的电阻；将电流表外接，则 （公式见原题） ___________流过元件的电流， （公式见原题） ___________元件的电阻。（均选填“小于”或“大于”）<br>（2）图（a）是某实验小组用电流表内接法测得的某元件的伏安特性曲线，由图可知，所测元件是__________（选填“线性”或“非线性”）电阻元件。随着电流的增加，元件的电阻__________（选填“增大”“不变”或“减小”）。<br>（3）利用电流表 （公式见原题） （内阻 （公式见原题） ）、电流表 （公式见原题） （内阻未知）以及一个用作保护电阻的定值电阻 （公式见原题） （阻值未知），测量电阻 （公式见原题） 的阻值。将图（b）中的器材符号的连线补充完整，完成实验电路原理图__________。按完整的实验电路测量 （公式见原题） ，某次测量中电流表 （公式见原题） 和 （公式见原题） 的示数分别为 （公式见原题） 和 （公式见原题） ，则 （公式见原题） ___________（用 （公式见原题） 和 （公式见原题） 表示）。<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_007.png\" alt=\"题目配图\"><img src=\"/images/questions/2025/physics/img_009.png\" alt=\"题目配图\"><img src=\"/images/questions/2025/physics/img_010.png\" alt=\"题目配图\"></div>', NULL, '（1） ①. 大于 ②. 大于 ③. 大于 ④. 小于<br>（2） ①. 非线性 ②. 减小<br>（3） ①. ②. （公式见原题）', '参考答案：<br>（1） ①. 大于 ②. 大于 ③. 大于 ④. 小于<br>（2） ①. 非线性 ②. 减小<br>（3） ①. ②. （公式见原题）', 4, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (127, 4, '127', 'ESSAY', '【2025高考课标卷物理·第11题】<br>11. 电容器 形状变化会导致其电容变化，这一性质可用于设计键盘，简化原理图如图所示。键盘按键下的装置可视为平行板电容器，电容器的极板面积为 （公式见原题） 、间距为 （公式见原题） ，电容 （公式见原题） （ （公式见原题） 为常量）。按下键盘按键时，极板间的距离变为按压前的 （公式见原题） 倍；撤去按压，按键在弹力作用下复位。电容器充电后：<br>（1）若按压按键不改变电容器所带的电荷量，则按压后极板间的电压变为按压前的多少倍？<br>（2）若按压按键不改变电容器极板间的电压，则按压后极板间的电场强度大小变为按压前的多少倍？<div class=\"question-images\"><img src=\"/images/questions/2025/physics/img_008.png\" alt=\"题目配图\"></div>', NULL, '（1） （公式见原题） 倍<br>（2） （公式见原题） 倍', '参考答案：<br>（1） （公式见原题） 倍<br>（2） （公式见原题） 倍', 4, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');
INSERT INTO `question` VALUES (128, 4, '128', 'ESSAY', '【2025高考课标卷物理·第12题】<br>12. 如图，物块P固定在水平面上，其上表面有半径为R的 （公式见原题） 圆弧轨道。P右端与薄板Q连在一起，圆弧轨道与Q上表面平滑连接。一轻弹簧的右端固定在Q上，另一端自由。质量为m的小球自圆弧顶端A点上方的B点自由下落，落到A点后沿圆弧轨道下滑，小球与弹簧接触后，当速度减小至刚接触时的 （公式见原题） 时弹簧的弹性势能为2mgR，此时断开P和Q的连接，从静止开始向右滑动。g为重力加速度大小，忽略空气阻力，圆弧轨道及Q的上、下表面均光滑，弹簧长度的变化始终在弹性限度内。<br>（1）求小球从落入圆弧轨道至离开圆弧轨道，重力对其做的功；<br>（2）求小球与弹簧刚接触时速度的大小及B、A两点间的距离；<br>（3）欲使P和Q断开后，弹簧的最大弹性势能等于2.2mgR，Q的质量应为多大？<br>（4）欲使P和Q断开后，Q的最终动能最大，Q的质量应为多大？', NULL, '（1） （公式见原题）<br>（2） （公式见原题） ， （公式见原题）<br>（3） （公式见原题）<br>（4） （公式见原题）', '参考答案：<br>（1） （公式见原题）<br>（2） （公式见原题） ， （公式见原题）<br>（3） （公式见原题）<br>（4） （公式见原题）', 4, '2025高考课标卷物理', 1, '2026-05-28 09:45:57', '2026-05-28 09:45:57');

-- ----------------------------
-- Table structure for study_note
-- ----------------------------
DROP TABLE IF EXISTS `study_note`;
CREATE TABLE `study_note`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `subject_id` bigint NULL DEFAULT NULL,
  `knowledge_id` bigint NULL DEFAULT NULL,
  `is_public` tinyint NULL DEFAULT 0,
  `view_count` int NULL DEFAULT 0,
  `like_count` int NULL DEFAULT 0,
  `collect_count` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_subject`(`subject_id` ASC) USING BTREE,
  INDEX `idx_public`(`is_public` ASC) USING BTREE,
  CONSTRAINT `study_note_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `study_note_ibfk_2` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of study_note
-- ----------------------------
INSERT INTO `study_note` VALUES (1, 2, '还真是', 'nb', NULL, NULL, 1, 0, 7, 0, '2026-03-30 14:35:32', '2026-03-30 14:35:32');
INSERT INTO `study_note` VALUES (2, 2, '怎么说呢', '实施效果\n', NULL, NULL, 1, 0, 0, 0, '2026-03-30 15:56:16', '2026-03-30 15:56:16');
INSERT INTO `study_note` VALUES (3, 4, '轻微发热他', '去WEAR', NULL, NULL, 1, 0, 1, 0, '2026-03-30 17:23:37', '2026-03-30 17:23:37');
INSERT INTO `study_note` VALUES (4, 6, '你好', 'cnm', NULL, NULL, 1, 0, 1, 0, '2026-05-27 10:04:17', '2026-05-27 10:04:17');
INSERT INTO `study_note` VALUES (5, 6, '你几把谁', '我草泥马\n', NULL, NULL, 0, 0, 1, 0, '2026-05-28 11:30:35', '2026-05-28 11:30:35');

-- ----------------------------
-- Table structure for subject
-- ----------------------------
DROP TABLE IF EXISTS `subject`;
CREATE TABLE `subject`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort_order` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of subject
-- ----------------------------
INSERT INTO `subject` VALUES (1, '语文', '语文科目', 'book', 1, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (2, '数学', '数学科目', 'calculator', 2, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (3, '英语', '英语科目', 'language', 3, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (4, '物理', '物理科目', 'atom', 4, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (5, '化学', '化学科目', 'flask', 5, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (6, '生物', '生物科目', 'dna', 6, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (7, '历史', '历史科目', 'history', 7, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (8, '地理', '地理科目', 'earth', 8, '2026-03-26 15:58:12');
INSERT INTO `subject` VALUES (9, '政治', '政治科目', 'balance', 9, '2026-03-26 15:58:12');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role` enum('STUDENT','TEACHER','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'STUDENT',
  `status` tinyint NULL DEFAULT 1 COMMENT '0-disabled, 1-active',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '高一/高二/高三',
  `age` int NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', NULL, NULL, NULL, 'ADMIN', 1, NULL, NULL, '2026-03-26 15:58:12', '2026-03-26 15:58:12');
INSERT INTO `user` VALUES (2, 'zhb', '$2a$10$ZtBqUgoJX5EIdJBdrGm5TupTedSXVO6uBGZWxfX0xdVBHRq9hBJBq', 'woc', NULL, NULL, NULL, 'STUDENT', 1, NULL, NULL, '2026-03-30 14:29:48', '2026-04-01 09:11:39');
INSERT INTO `user` VALUES (3, 'test', '$2a$10$ZtBqUgoJX5EIdJBdrGm5TupTedSXVO6uBGZWxfX0xdVBHRq9hBJBq', '测试用户', NULL, 'test@example.com', NULL, 'STUDENT', 1, NULL, NULL, '2026-03-30 14:45:43', '2026-03-30 14:45:43');
INSERT INTO `user` VALUES (4, '123', '$2a$10$ydhVNaJDPuF98qJrpbQ4vurJAtTxQgwSzqxjctrKhIZDoAbIoLQj.', '123', NULL, NULL, NULL, 'STUDENT', 1, NULL, NULL, '2026-03-30 16:12:27', '2026-03-30 16:12:27');
INSERT INTO `user` VALUES (5, 'zzj', '$2a$10$2o6LoaSHDfQdNU8Lvy/B6.bo0t.GaBTae8lc0.eSXC7MBLQENJ/nK', 'zzj', NULL, NULL, NULL, 'STUDENT', 1, NULL, NULL, '2026-04-02 16:19:52', '2026-04-02 16:19:52');
INSERT INTO `user` VALUES (6, 'jht', '$2a$10$Xl498I.8URKeQBk9c8Ins.uYlsxc4szBScmkznxVF4kVZpUsPAeB.', 'jht', NULL, '2819516357@qq.com', '77777', 'STUDENT', 1, '高二', 7, '2026-05-06 17:21:31', '2026-05-06 17:21:31');
INSERT INTO `user` VALUES (10, 'testuser', '$2a$10$bPUqjq6mIwnWhbynoaqA/OxWao70my7U.U1ROhq0b3hiDdb/pKx5a', 'testuser', NULL, 'test@test.com', NULL, 'STUDENT', 1, NULL, NULL, '2026-05-20 09:58:33', '2026-05-20 09:58:33');

-- ----------------------------
-- Table structure for view_record
-- ----------------------------
DROP TABLE IF EXISTS `view_record`;
CREATE TABLE `view_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_type` enum('POST','NOTE','COURSE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_target`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of view_record
-- ----------------------------
INSERT INTO `view_record` VALUES (1, 2, 'POST', 1, '2026-03-30 17:05:45');
INSERT INTO `view_record` VALUES (2, 3, 'POST', 1, '2026-04-01 09:10:08');
INSERT INTO `view_record` VALUES (3, 6, 'POST', 1, '2026-05-06 17:22:59');
INSERT INTO `view_record` VALUES (4, 6, 'POST', 3, '2026-05-26 08:38:07');
INSERT INTO `view_record` VALUES (5, 6, 'COURSE', 1, '2026-05-26 11:35:18');
INSERT INTO `view_record` VALUES (6, 6, 'COURSE', 4, '2026-05-26 11:35:24');
INSERT INTO `view_record` VALUES (7, 6, 'POST', 4, '2026-05-30 18:17:23');

SET FOREIGN_KEY_CHECKS = 1;
