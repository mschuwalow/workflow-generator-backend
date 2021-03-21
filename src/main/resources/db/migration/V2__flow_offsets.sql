CREATE TABLE flow_offsets (
  flow_id       UUID   NOT NULL,
  component_id  TEXT   NOT NULL,
  stream_offset BIGINT NOT NULL,
  PRIMARY KEY(flow_id, component_id),
  CONSTRAINT fk_flow_id
    FOREIGN KEY(flow_id) REFERENCES flows(flow_id) ON DELETE CASCADE
);
