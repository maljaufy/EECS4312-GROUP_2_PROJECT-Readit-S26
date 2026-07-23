-- Reddit Clone Database Schema - User Module and Related Tables
-- PostgreSQL Schema for User entity, posts, comments, voting, and outbox

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS votes CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS outbox_event CASCADE;
DROP TABLE IF EXISTS processed_events CASCADE;
DROP TABLE IF EXISTS subreddits CASCADE;
DROP TABLE IF EXISTS users_roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    karma INTEGER NOT NULL DEFAULT 0,
    bio VARCHAR(500),
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at for users table
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default admin user (password should be hashed in production)
-- This is just for initial setup - change the password immediately
INSERT INTO users (username, email, password_hash, karma, created_by, updated_by)
VALUES ('admin', 'admin@readit.com', '$2a$10$placeholder_hash_change_me', 0, 'system', 'system');

-- Outbox event table for event sourcing pattern
CREATE TABLE outbox_event (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    error_message VARCHAR(500)
);

CREATE INDEX idx_outbox_event_status ON outbox_event(status);
CREATE INDEX idx_outbox_event_created_at ON outbox_event(created_at);

-- Idempotency ledger shared by event handlers.  The unique key makes the
-- reservation atomic even when Kafka redelivers concurrently.
CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    handler_name VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_processed_event_handler UNIQUE (event_id, handler_name)
);

CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);

-- Subreddits table
CREATE TABLE subreddits (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_subreddits_name ON subreddits(name);

-- Trigger to automatically update updated_at for subreddits table
CREATE TRIGGER update_subreddits_updated_at BEFORE UPDATE ON subreddits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Posts table
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    content VARCHAR(10000),
    author_id BIGINT NOT NULL,
    subreddit_id BIGINT NOT NULL,
    vote_score INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_subreddit FOREIGN KEY (subreddit_id) REFERENCES subreddits(id) ON DELETE CASCADE
);

CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_subreddit ON posts(subreddit_id);

-- Trigger to automatically update updated_at for posts table
CREATE TRIGGER update_posts_updated_at BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments table
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body VARCHAR(5000) NOT NULL,
    vote_score INTEGER NOT NULL DEFAULT 0,
    parent_comment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_post_created ON comments(post_id, created_at);
CREATE INDEX idx_comments_author ON comments(author_id);
CREATE INDEX idx_comments_body_search ON comments
    USING GIN (to_tsvector('english', COALESCE(body, '')));

-- Votes table
CREATE TABLE votes (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    voter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    post_target_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN target_type = 'POST' THEN target_id ELSE NULL END
    ) STORED,
    comment_target_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN target_type = 'COMMENT' THEN target_id ELSE NULL END
    ) STORED,
    vote_value VARCHAR(20) NOT NULL,
    CONSTRAINT chk_votes_target_type CHECK (target_type IN ('POST', 'COMMENT')),
    CONSTRAINT chk_votes_value CHECK (vote_value IN ('UPVOTE', 'DOWNVOTE')),
    CONSTRAINT fk_votes_voter FOREIGN KEY (voter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_post_target FOREIGN KEY (post_target_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_comment_target FOREIGN KEY (comment_target_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT uk_vote_voter_target UNIQUE (voter_id, target_type, target_id)
);

CREATE INDEX idx_votes_target ON votes(target_type, target_id);
CREATE INDEX idx_votes_voter ON votes(voter_id);

-- Users roles table (for @ElementCollection in User entity)
CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_parent ON comments(parent_comment_id);
