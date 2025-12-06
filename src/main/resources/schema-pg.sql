-- UUID 생성 함수 사용을 위해 확장 설치
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- USERS TABLE
CREATE TABLE IF NOT EXISTS users
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(512) NOT NULL,
    profile_image_url TEXT,
    role VARCHAR(20) NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    follower_count BIGINT NOT NULL DEFAULT 0
    );

-- CONTENTS TABLE
CREATE TABLE IF NOT EXISTS contents
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    thumbnail_url TEXT,
    content_type VARCHAR(255) NOT NULL,
    average_rating DOUBLE PRECISION DEFAULT 0.0,
    review_count INT DEFAULT 0,
    watcher_count INT DEFAULT 0
    );

-- CONTENT TAGS TABLE
CREATE TABLE IF NOT EXISTS contents_tags
(
    content_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,

    PRIMARY KEY (content_id, tag),
    FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
    );

-- PLAYLISTS TABLE
CREATE TABLE IF NOT EXISTS playlists
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    owner_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subscriber_count BIGINT NOT NULL DEFAULT 0,
    subscribed_by_me BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- PLAYLIST ITEMS TABLE
CREATE TABLE IF NOT EXISTS playlist_items
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    playlist_id UUID NOT NULL,
    content_id UUID NOT NULL,

    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
    );

-- PLAYLIST SUBSCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS playlist_subscriptions
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    subscriber_id UUID NOT NULL,
    playlist_id UUID NOT NULL,
    subscribed_at TIMESTAMP,

    FOREIGN KEY (subscriber_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    UNIQUE (subscriber_id, playlist_id)
    );

-- NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS notifications
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    level VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- REVIEWS TABLE
CREATE TABLE IF NOT EXISTS reviews
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    text TEXT NOT NULL,
    rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_deleted BOOLEAN,

    FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- CONVERSATIONS TABLE
CREATE TABLE IF NOT EXISTS conversations
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    with_user_id UUID NOT NULL,
    user_id UUID NOT NULL,
    has_unread BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (with_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- DIRECT MESSAGES TABLE
CREATE TABLE IF NOT EXISTS direct_messages
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    sender UUID NOT NULL,
    receiver UUID NOT NULL,
    conversation_id UUID NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (sender) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
    );

-- FOLLOWS TABLE
CREATE TABLE IF NOT EXISTS follows
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,

    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (follower_id, followee_id)
    );

-- WATCHING SESSION TABLE
CREATE TABLE IF NOT EXISTS watching_sessions
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    UNIQUE(user_id)
    );

-- PROCESSED EVENT
CREATE TABLE IF NOT EXISTS processed_events
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    event_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,

    UNIQUE(event_id, event_type)
    );
