/* Replace with your SQL commands */

CREATE TABLE flows (
       id SERIAL PRIMARY KEY,
       project_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
       name TEXT NOT NULL,
       description TEXT,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX flows_name_project_id ON flows (name, project_id);

CREATE TRIGGER update_flows_updated_at
BEFORE UPDATE
ON flows
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TABLE flow_events (
       id SERIAL PRIMARY KEY,
       name TEXT NOT NULL,
       description TEXT,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TRIGGER update_flow_events_updated_at
BEFORE UPDATE
ON flow_events
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TABLE flow_screens (
       id SERIAL PRIMARY KEY,
       name TEXT NOT NULL,
       description TEXT,
       project_file_id INTEGER REFERENCES project_files(id) ON DELETE CASCADE,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TRIGGER update_flow_screens_updated_at
BEFORE UPDATE
ON flow_screens
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TABLE flow_screen_hotspots (
       id SERIAL PRIMARY KEY,
       name TEXT NOT NULL,
       description TEXT,
       c_top FLOAT,
       c_bottom FLOAT,
       c_left FLOAT,
       c_right FLOAT,
       d_width FLOAT,
       d_height FLOAT,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TRIGGER update_flow_screen_hotspots_updated_at
BEFORE UPDATE
ON flow_screen_hotspots
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TABLE flow_switches (
       id SERIAL PRIMARY KEY,
       name TEXT NOT NULL,
       description TEXT,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TRIGGER update_flow_switches_updated_at
BEFORE UPDATE
ON flow_switches
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TABLE flow_switch_options (
       id SERIAL PRIMARY KEY,
       name TEXT NOT NULL,
       description TEXT,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TRIGGER update_flow_switch_options_updated_at
BEFORE UPDATE
ON flow_switch_options
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TABLE flow_flow_refs (
       id SERIAL PRIMARY KEY,
       target_flow_id INTEGER REFERENCES flows(id) ON DELETE SET NULL,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TRIGGER update_flow_flow_refs_updated_at
BEFORE UPDATE
ON flow_flow_refs
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TABLE flow_nodes (
       id SERIAL PRIMARY KEY,
       is_entrypoint BOOLEAN NOT NULL DEFAULT FALSE,
       flow_id INTEGER REFERENCES flows(id) ON DELETE CASCADE,
       flow_event_id INTEGER REFERENCES flow_events(id) ON DELETE CASCADE,
       flow_screen_id INTEGER REFERENCES flow_screens(id) ON DELETE CASCADE,
       flow_switch_id INTEGER REFERENCES flow_switches(id) ON DELETE CASCADE,
       flow_flow_ref_id INTEGER REFERENCES flow_flow_refs(id) ON DELETE CASCADE,
       CHECK (
         (
           (flow_event_id is not null)::integer +
           (flow_screen_id is not null)::integer +
           (flow_switch_id is not null)::integer +
           (flow_flow_ref_id is not null)::integer 
         ) = 1
       )
);

CREATE UNIQUE INDEX ON flow_nodes (flow_event_id) WHERE flow_event_id IS NOT NULL;
CREATE UNIQUE INDEX ON flow_nodes (flow_screen_id) WHERE flow_screen_id IS NOT NULL;
CREATE UNIQUE INDEX ON flow_nodes (flow_switch_id) WHERE flow_switch_id IS NOT NULL;
CREATE UNIQUE INDEX ON flow_nodes (flow_flow_ref_id) WHERE flow_flow_ref_id IS NOT NULL;
CREATE UNIQUE INDEX ON flow_nodes (is_entrypoint, flow_id) WHERE is_entrypoint = TRUE;

CREATE OR REPLACE FUNCTION delete_flow_nodes_related_node() RETURNS TRIGGER AS
$$BEGIN
  IF OLD.flow_event_id IS NOT NULL THEN
     DELETE FROM flow_events WHERE id = OLD.flow_event_id;
  END IF;
  IF OLD.flow_screen_id IS NOT NULL THEN
     DELETE FROM flow_screens WHERE id = OLD.flow_screen_id;
  END IF;
  IF OLD.flow_switch_id IS NOT NULL THEN
     DELETE FROM flow_switches WHERE id = OLD.flow_switch_id;
  END IF;
  IF OLD.flow_flow_ref_id IS NOT NULL THEN
     DELETE FROM flow_flow_refs WHERE id = OLD.flow_flow_ref_id;
  END IF;
  RETURN OLD;
END;$$ LANGUAGE plpgsql;

CREATE TRIGGER delete_flow_nodes_related_node
   AFTER DELETE ON flow_nodes FOR EACH ROW
   EXECUTE PROCEDURE delete_flow_nodes_related_node();

ALTER TABLE flow_events
ADD COLUMN target_flow_node_id INTEGER REFERENCES flow_nodes(id) ON DELETE SET NULL;

ALTER TABLE flow_screen_hotspots
ADD COLUMN target_flow_node_id INTEGER REFERENCES flow_nodes(id) ON DELETE SET NULL;

ALTER TABLE flow_switch_options
ADD COLUMN target_flow_node_id INTEGER REFERENCES flow_nodes(id) ON DELETE SET NULL;
