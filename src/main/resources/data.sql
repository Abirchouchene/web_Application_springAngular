-- Database schema fix for enum columns
-- This script will be executed automatically by Spring Boot

-- Fix priority column size in request table
ALTER TABLE request MODIFY COLUMN priority VARCHAR(20);

-- Fix status column size in request table  
ALTER TABLE request MODIFY COLUMN status VARCHAR(20);

-- Fix request_type column size in request table
ALTER TABLE request MODIFY COLUMN request_type VARCHAR(20);

-- Fix category_request column size in request table
ALTER TABLE request MODIFY COLUMN category_request VARCHAR(50);

-- Fix question_type column: was ENUM('STATISTICS','INFORMATION','TEXT','CHOICE'), needs VARCHAR for new QuestionType values
ALTER TABLE question MODIFY COLUMN question_type VARCHAR(50);

-- Fix role column in user table: may be ENUM with limited values, needs VARCHAR for all Role enum values
ALTER TABLE user MODIFY COLUMN role VARCHAR(30);

-- Fix any existing bad data: update questions with empty question_type to SHORT_ANSWER
UPDATE question SET question_type = 'SHORT_ANSWER' WHERE question_type = '' OR question_type IS NULL; 