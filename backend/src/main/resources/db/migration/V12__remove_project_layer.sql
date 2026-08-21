-- Remove the intermediate Project layer to simplify:
-- OLD: Workspace -> Project -> Board -> List -> Task
-- NEW: Workspace -> Board -> List -> Task

-- 1. Add workspace_id to boards and backfill from projects
ALTER TABLE boards ADD workspace_id BINARY(16) NULL;

UPDATE boards b
  INNER JOIN projects p ON b.project_id = p.id
  SET b.workspace_id = p.workspace_id;

ALTER TABLE boards MODIFY workspace_id BINARY(16) NOT NULL;

ALTER TABLE boards ADD CONSTRAINT fk_boards_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
    ON DELETE CASCADE;

CREATE INDEX idx_boards_workspace ON boards (workspace_id, deleted_at);

-- 2. Add workspace_id to tags and backfill from projects
ALTER TABLE tags ADD workspace_id BINARY(16) NULL;

UPDATE tags t
  INNER JOIN projects p ON t.project_id = p.id
  SET t.workspace_id = p.workspace_id;

ALTER TABLE tags MODIFY workspace_id BINARY(16) NOT NULL;

-- Must drop FK constraint before dropping the index it depends on
ALTER TABLE tags DROP FOREIGN KEY fk_tags_project;
ALTER TABLE tags DROP INDEX uq_tags_project_name;
ALTER TABLE tags ADD CONSTRAINT uq_tags_workspace_name UNIQUE (workspace_id, name);

ALTER TABLE tags ADD CONSTRAINT fk_tags_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
    ON DELETE CASCADE;

-- 3. Drop old FK constraints and columns
ALTER TABLE boards DROP FOREIGN KEY fk_boards_project;
ALTER TABLE boards DROP COLUMN project_id;

ALTER TABLE tags DROP COLUMN project_id;

-- 4. Drop project tables (order: members first due to FK)
DROP TABLE IF EXISTS project_members;
DROP TABLE IF EXISTS projects;
