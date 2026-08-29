CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE roadmap (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    source VARCHAR(50) NOT NULL,
    raw_text TEXT,
    parsed_structure JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chapter (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    roadmap_id UUID NOT NULL REFERENCES roadmap(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    order_index INT NOT NULL
);

CREATE TABLE sub_chapter (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chapter_id UUID NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    order_index INT NOT NULL,
    topics JSONB
);

CREATE TABLE generation_job (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    roadmap_id UUID NOT NULL REFERENCES roadmap(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    requested_scope VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE artifact (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    logical_name VARCHAR(255) NOT NULL,
    storage_location TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(255),
    merged_chapter_artifact_id UUID REFERENCES artifact(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE generation_task (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    generation_job_id UUID NOT NULL REFERENCES generation_job(id) ON DELETE CASCADE,
    sub_chapter_id UUID REFERENCES sub_chapter(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    agent_role VARCHAR(50) NOT NULL,
    step_description VARCHAR(255),
    retry_count INT DEFAULT 0,
    current_attempt_id UUID,
    output_artifact_id UUID REFERENCES artifact(id),
    error_detail TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE generation_attempt (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    generation_task_id UUID NOT NULL REFERENCES generation_task(id) ON DELETE CASCADE,
    provider VARCHAR(100),
    model VARCHAR(100),
    prompt_reference TEXT,
    response_reference TEXT,
    duration_ms BIGINT,
    outcome VARCHAR(50),
    validation_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE generation_task ADD CONSTRAINT fk_current_attempt FOREIGN KEY (current_attempt_id) REFERENCES generation_attempt(id) ON DELETE SET NULL;
