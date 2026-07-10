-- Sample data for Reddit Clone
-- Run this AFTER schema.sql has been applied.
-- Assumes the 'admin' user (id=1) already exists from schema.sql's seed insert.

-- ============================================================
-- USERS
-- ============================================================
INSERT INTO users (username, email, password_hash, karma, bio, created_by, updated_by) VALUES
('alice_codes',   'alice@example.com',   '$2a$10$placeholder_hash_change_me', 142, 'Backend dev, coffee enjoyer.', 'system', 'system'),
('bob_builds',    'bob@example.com',     '$2a$10$placeholder_hash_change_me', 87,  'I like building things that break.', 'system', 'system'),
('carla_designs', 'carla@example.com',   '$2a$10$placeholder_hash_change_me', 213, 'UX nerd. Ask me about accessibility.', 'system', 'system'),
('dave_lurks',    'dave@example.com',    '$2a$10$placeholder_hash_change_me', 12,  NULL, 'system', 'system');

-- Give each of these standard USER role (admin already has whatever schema.sql assigned, if anything)
INSERT INTO users_roles (user_id, role)
SELECT id, 'USER' FROM users WHERE username IN ('alice_codes', 'bob_builds', 'carla_designs', 'dave_lurks');

-- ============================================================
-- SUBREDDITS
-- ============================================================
INSERT INTO subreddits (name, description, is_private, created_by, updated_by) VALUES
('java',        'All things Java development.', false, 'system', 'system'),
('webdev',      'Frontend, backend, and everything in between.', false, 'system', 'system'),
('askprogramming', 'Programming questions of any skill level.', false, 'system', 'system');

-- ============================================================
-- POSTS
-- ============================================================
INSERT INTO posts (title, content, author_id, subreddit_id, created_by, updated_by)
SELECT
    'Why does @Transactional not work when calling a method on the same class?',
    'I have a service method calling another method in the same class, both annotated with @Transactional on the inner one. The transaction never actually starts. What am I missing?',
    u.id, s.id, u.username, u.username
FROM users u, subreddits s
WHERE u.username = 'alice_codes' AND s.name = 'java';

INSERT INTO posts (title, content, author_id, subreddit_id, created_by, updated_by)
SELECT
    'Vaadin vs React for an internal admin tool - worth it in 2026?',
    'Team is deciding between building an internal dashboard in Vaadin (pure Java, no separate frontend) vs a React app with a REST API. Curious about real experiences with maintenance cost over time.',
    u.id, s.id, u.username, u.username
FROM users u, subreddits s
WHERE u.username = 'bob_builds' AND s.name = 'webdev';

INSERT INTO posts (title, content, author_id, subreddit_id, created_by, updated_by)
SELECT
    'How do you decide between polymorphic association vs separate FK columns?',
    'Working on a voting system where a vote can target either a post or a comment. Trying to decide between one votes table with a target_type discriminator vs two nullable FK columns. Tradeoffs?',
    u.id, s.id, u.username, u.username
FROM users u, subreddits s
WHERE u.username = 'carla_designs' AND s.name = 'askprogramming';

-- ============================================================
-- COMMENTS (mix of top-level and nested replies)
-- ============================================================

-- Top-level comment on post 1
INSERT INTO comments (post_id, author_id, body, created_by, updated_by)
SELECT p.id, u.id, 'This is almost always because Spring proxies intercept calls from OUTSIDE the bean - a self-call bypasses the proxy entirely.', u.username, u.username
FROM posts p, users u
WHERE p.title LIKE 'Why does @Transactional%' AND u.username = 'bob_builds';

-- Reply to the comment above (nested, depth 1)
INSERT INTO comments (post_id, author_id, body, parent_comment_id, created_by, updated_by)
SELECT p.id, u.id, 'This is the answer. Split the @Transactional method into a separate bean and inject it, or use self-injection.', c.id, u.username, u.username
FROM posts p, users u, comments c
WHERE p.title LIKE 'Why does @Transactional%'
  AND u.username = 'carla_designs'
  AND c.body LIKE 'This is almost always%';

-- Reply to the reply (nested, depth 2)
INSERT INTO comments (post_id, author_id, body, parent_comment_id, created_by, updated_by)
SELECT p.id, u.id, 'Self-injection feels like a hack but I have seen it in production more than once.', c.id, u.username, u.username
FROM posts p, users u, comments c
WHERE p.title LIKE 'Why does @Transactional%'
  AND u.username = 'alice_codes'
  AND c.body LIKE 'This is the answer%';

-- Another top-level comment on post 1 (flat, no replies)
INSERT INTO comments (post_id, author_id, body, created_by, updated_by)
SELECT p.id, u.id, 'Had this exact bug last week. Cost me an afternoon.', u.username, u.username
FROM posts p, users u
WHERE p.title LIKE 'Why does @Transactional%' AND u.username = 'dave_lurks';

-- Top-level comment on post 2
INSERT INTO comments (post_id, author_id, body, created_by, updated_by)
SELECT p.id, u.id, 'For an internal tool with a small team, Vaadin removes an entire layer of complexity - no separate API contract to maintain.', u.username, u.username
FROM posts p, users u
WHERE p.title LIKE 'Vaadin vs React%' AND u.username = 'alice_codes';

-- Reply to that comment
INSERT INTO comments (post_id, author_id, body, parent_comment_id, created_by, updated_by)
SELECT p.id, u.id, 'Agreed, but you lose the ability to reuse the same backend for a future mobile app without adding a REST layer later anyway.', c.id, u.username, u.username
FROM posts p, users u, comments c
WHERE p.title LIKE 'Vaadin vs React%'
  AND u.username = 'carla_designs'
  AND c.body LIKE 'For an internal tool%';

-- Top-level comment on post 3
INSERT INTO comments (post_id, author_id, body, created_by, updated_by)
SELECT p.id, u.id, 'Polymorphic association loses real DB-level referential integrity unless you add generated columns + separate FKs like Postgres lets you do.', u.username, u.username
FROM posts p, users u
WHERE p.title LIKE 'How do you decide between polymorphic%' AND u.username = 'bob_builds';

-- ============================================================
-- VOTES (polymorphic target_type/target_id design)
-- ============================================================

-- Upvotes on post 1
INSERT INTO votes (voter_id, target_type, target_id, value)
SELECT u.id, 'POST', p.id, 'UPVOTE'
FROM users u, posts p
WHERE p.title LIKE 'Why does @Transactional%'
  AND u.username IN ('bob_builds', 'carla_designs', 'dave_lurks');

-- One downvote on post 2 (controversial-ish)
INSERT INTO votes (voter_id, target_type, target_id, value)
SELECT u.id, 'POST', p.id, 'DOWNVOTE'
FROM users u, posts p
WHERE p.title LIKE 'Vaadin vs React%' AND u.username = 'dave_lurks';

INSERT INTO votes (voter_id, target_type, target_id, value)
SELECT u.id, 'POST', p.id, 'UPVOTE'
FROM users u, posts p
WHERE p.title LIKE 'Vaadin vs React%' AND u.username = 'carla_designs';

-- Upvotes on the top comment in post 1's thread
INSERT INTO votes (voter_id, target_type, target_id, value)
SELECT u.id, 'COMMENT', c.id, 'UPVOTE'
FROM users u, comments c
WHERE c.body LIKE 'This is almost always%'
  AND u.username IN ('alice_codes', 'carla_designs', 'dave_lurks');

-- A downvote on the "self-injection feels like a hack" reply
INSERT INTO votes (voter_id, target_type, target_id, value)
SELECT u.id, 'COMMENT', c.id, 'DOWNVOTE'
FROM users u, comments c
WHERE c.body LIKE 'Self-injection feels like a hack%'
  AND u.username = 'bob_builds';