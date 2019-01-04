/* Replace with your SQL commands */

ALTER TABLE project_files
ADD COLUMN project_id INTEGER REFERENCES projects(id) ON DELETE CASCADE;
