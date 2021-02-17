CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE forms (
  form_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  elements JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE flows (
  flow_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  streams JSONB NOT NULL,
  state VARCHAR NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
