-- Fix notification type column to accommodate CALLBACK value
ALTER TABLE notification MODIFY COLUMN type VARCHAR(50); 