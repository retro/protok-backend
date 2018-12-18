/* Replace with your SQL commands */

CREATE TABLE projects (
       id SERIAL PRIMARY KEY,
       organization_id INTEGER REFERENCES organizations(id) ON DELETE CASCADE,
       name TEXT NOT NULL,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX projects_name_organization_id ON projects (name, organization_id);

CREATE TRIGGER update_projects_updated_at
BEFORE UPDATE
ON projects
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();
