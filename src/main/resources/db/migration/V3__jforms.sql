CREATE TABLE jforms (
  jform_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  data_schema JSONB NOT NULL,
  ui_schema JSONB NOT NULL,
  perms JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
