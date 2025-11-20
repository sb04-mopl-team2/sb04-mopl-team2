-- USERS TABLE
CREATE TABLE IF NOT EXISTS users
(
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(512) NOT NULL,
    profile_image_url TEXT,
    role VARCHAR(20) NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    follower_count BIGINT NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS contents
(
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    thumbnail_url TEXT,
    average_rating DOUBLE DEFAULT 0.0,
    review_count INT DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS contents_tags
(
    content_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (content_id, tag)
    );

CREATE TABLE IF NOT EXISTS playlists
(
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP,
    owner_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subscriber_count BIGINT NOT NULL DEFAULT 0,
    subscribe_by_me BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS playlist_items
(
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    playlist_id UUID NOT NULL,
    content_id UUID NOT NULL
    );

CREATE TABLE IF NOT EXISTS notifications
(
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    level VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD'
    );

CREATE TABLE IF NOT EXISTS reviews
(
    id UUID PRIMARY KEY,
    text TEXT NOT NULL,
    rating DOUBLE NOT NULL DEFAULT 0.0,
    content_id UUID NOT NULL,
    user_id UUID NOT NULL
    );

CREATE TABLE IF NOT EXISTS conversations
(
    id UUID PRIMARY KEY,
    with_user_id UUID NOT NULL,
    user_id UUID NOT NULL,
    has_unread BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS direct_messages
(
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP,
    sender UUID NOT NULL,
    receiver UUID NOT NULL,
    conversation_id UUID NOT NULL,
    content TEXT NOT NULL
    );

CREATE TABLE IF NOT EXISTS follows
(
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    UNIQUE (follower_id, followee_id)
    );

ALTER TABLE follows
    ADD CONSTRAINT no_self_follow CHECK (follower_id != followee_id);
