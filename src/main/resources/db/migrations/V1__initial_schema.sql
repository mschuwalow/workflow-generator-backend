CREATE TABLE workflows (
  ID BIGSERIAL PRIMARY KEY,
  flow VARCHAR NOT NULL,
  state VARCHAR NOT NULL
);
