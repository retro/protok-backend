/* Replace with your SQL commands */

CREATE TYPE project_file_file_type_t AS ENUM('screen', 'attachment');

CREATE TABLE project_files (
       id SERIAL PRIMARY KEY,
       project_file_group_id INTEGER REFERENCES project_file_groups(id) ON DELETE CASCADE,
       filename TEXT NOT NULL,
       description TEXT NOT NULL DEFAULT '',
       mime_type TEXT NOT NULL,
       file_type project_file_file_type_t NOT NULL DEFAULT 'attachment',
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TRIGGER update_project_files_updated_at
BEFORE UPDATE
ON project_files
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();
