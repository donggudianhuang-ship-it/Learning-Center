-- Add FULLTEXT index for community post search
-- Requires MySQL 5.7.6+ for ngram parser (Chinese word segmentation)
-- If ngram is not supported, remove "WITH PARSER ngram" - falls back to word-based tokenizer
ALTER TABLE community_post ADD FULLTEXT INDEX ft_post_search (title, content) WITH PARSER ngram;
