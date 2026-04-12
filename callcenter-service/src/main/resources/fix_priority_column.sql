-- Fix priority column size in request table
-- This script increases the size of the priority column to accommodate all enum values

ALTER TABLE request MODIFY COLUMN priority VARCHAR(20);

-- Also fix other enum columns that might have similar issues
ALTER TABLE request MODIFY COLUMN status VARCHAR(20);
ALTER TABLE request MODIFY COLUMN request_type VARCHAR(20);
ALTER TABLE request MODIFY COLUMN category_request VARCHAR(50); 