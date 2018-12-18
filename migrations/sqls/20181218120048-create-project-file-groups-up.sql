/* Replace with your SQL commands */

CREATE TABLE project_file_groups (
       id SERIAL PRIMARY KEY,
       project_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
       name TEXT NOT NULL,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX project_file_groups_name_project_id ON project_file_groups (name, project_id);

CREATE TRIGGER update_project_file_groups_updated_at
BEFORE UPDATE
ON project_file_groups
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();
