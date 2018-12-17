/* Replace with your SQL commands */

CREATE TABLE organizations (
       id SERIAL PRIMARY KEY,
       name TEXT NOT NULL UNIQUE,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX organizations_name ON organizations (name);

CREATE TRIGGER update_organizations_updated_at
BEFORE UPDATE
ON organizations
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TYPE organization_member_role_t AS ENUM('owner', 'admin', 'member');

CREATE TABLE organization_members (
       account_id INTEGER REFERENCES accounts(id) ON DELETE CASCADE,
       organization_id INTEGER REFERENCES organizations(id) ON DELETE CASCADE,
       member_role organization_member_role_t NOT NULL DEFAULT 'member',
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW(),
       PRIMARY KEY (organization_id, account_id)
);

CREATE UNIQUE INDEX organization_members_owner ON organization_members (organization_id, member_role)
    WHERE member_role = 'owner';

CREATE TRIGGER update_organization_members_updated_at
BEFORE UPDATE
ON organization_members
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

