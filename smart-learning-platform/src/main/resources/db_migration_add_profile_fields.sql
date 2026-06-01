-- Add grade and age columns to user table
-- Run this if the database already exists

USE smart_learning;

-- Add grade column if not exists
SET @exist_grade := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = 'smart_learning'
                     AND TABLE_NAME = 'user'
                     AND COLUMN_NAME = 'grade');
SET @sql_grade := IF(@exist_grade = 0,
    'ALTER TABLE user ADD COLUMN grade VARCHAR(20) COMMENT ''高一/高二/高三'' AFTER status',
    'SELECT ''grade column already exists''');
PREPARE stmt FROM @sql_grade;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add age column if not exists
SET @exist_age := (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'smart_learning'
                   AND TABLE_NAME = 'user'
                   AND COLUMN_NAME = 'age');
SET @sql_age := IF(@exist_age = 0,
    'ALTER TABLE user ADD COLUMN age INT AFTER grade',
    'SELECT ''age column already exists''');
PREPARE stmt FROM @sql_age;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
