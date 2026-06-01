USE smart_learning;

ALTER TABLE knowledge_mastery
    MODIFY mastery_level DECIMAL(5,2) DEFAULT 0 COMMENT '0-100 percentage';
