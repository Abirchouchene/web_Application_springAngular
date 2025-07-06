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