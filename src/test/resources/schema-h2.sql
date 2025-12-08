-- USERS TABLE
CREATE TABLE IF NOT EXISTS users
(
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(512) NOT NULL,
    profile_image_url TEXT,
    role VARCHAR(20) NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    follower_count BIGINT NOT NULL DEFAULT 0
    );

-- CONTENTS TABLE
CREATE TABLE IF NOT EXISTS contents
(
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    thumbnail_url TEXT,
    content_type VARCHAR(255) NOT NULL,
    average_rating DOUBLE DEFAULT 0.0,
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
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    playlist_id UUID NOT NULL,
    content_id UUID NOT NULL,

    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE
    );

-- PLAYLIST SUBSCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS playlist_subscriptions
(
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    text TEXT NOT NULL,
    rating DOUBLE NOT NULL DEFAULT 0.0,
    is_deleted BOOLEAN,

    FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- CONVERSATIONS TABLE
CREATE TABLE IF NOT EXISTS conversations
(
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,

    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (follower_id, followee_id)
    );

-- WATCHING SESSION TABLE
CREATE TABLE IF NOT EXISTS watching_sessions
(
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,

    UNIQUE(event_id, event_type)
    );

ALTER TABLE follows
    ADD CONSTRAINT no_self_follow CHECK (follower_id != followee_id);



-- Autogenerated: do not edit this file

CREATE TABLE BATCH_JOB_INSTANCE  (
                                     JOB_INSTANCE_ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY ,
                                     VERSION BIGINT ,
                                     JOB_NAME VARCHAR(100) NOT NULL,
                                     JOB_KEY VARCHAR(32) NOT NULL,
                                     constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
) ;

CREATE TABLE BATCH_JOB_EXECUTION  (
                                      JOB_EXECUTION_ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY ,
                                      VERSION BIGINT  ,
                                      JOB_INSTANCE_ID BIGINT NOT NULL,
                                      CREATE_TIME TIMESTAMP(9) NOT NULL,
                                      START_TIME TIMESTAMP(9) DEFAULT NULL ,
                                      END_TIME TIMESTAMP(9) DEFAULT NULL ,
                                      STATUS VARCHAR(10) ,
                                      EXIT_CODE VARCHAR(2500) ,
                                      EXIT_MESSAGE VARCHAR(2500) ,
                                      LAST_UPDATED TIMESTAMP(9),
                                      constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
                                          references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
                                             JOB_EXECUTION_ID BIGINT NOT NULL ,
                                             PARAMETER_NAME VARCHAR(100) NOT NULL ,
                                             PARAMETER_TYPE VARCHAR(100) NOT NULL ,
                                             PARAMETER_VALUE VARCHAR(2500) ,
                                             IDENTIFYING CHAR(1) NOT NULL ,
                                             constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
                                                 references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION  (
                                       STEP_EXECUTION_ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY ,
                                       VERSION BIGINT NOT NULL,
                                       STEP_NAME VARCHAR(100) NOT NULL,
                                       JOB_EXECUTION_ID BIGINT NOT NULL,
                                       CREATE_TIME TIMESTAMP(9) NOT NULL,
                                       START_TIME TIMESTAMP(9) DEFAULT NULL ,
                                       END_TIME TIMESTAMP(9) DEFAULT NULL ,
                                       STATUS VARCHAR(10) ,
                                       COMMIT_COUNT BIGINT ,
                                       READ_COUNT BIGINT ,
                                       FILTER_COUNT BIGINT ,
                                       WRITE_COUNT BIGINT ,
                                       READ_SKIP_COUNT BIGINT ,
                                       WRITE_SKIP_COUNT BIGINT ,
                                       PROCESS_SKIP_COUNT BIGINT ,
                                       ROLLBACK_COUNT BIGINT ,
                                       EXIT_CODE VARCHAR(2500) ,
                                       EXIT_MESSAGE VARCHAR(2500) ,
                                       LAST_UPDATED TIMESTAMP(9),
                                       constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
                                           references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
                                               STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                                               SHORT_CONTEXT VARCHAR(2500) NOT NULL,
                                               SERIALIZED_CONTEXT LONGVARCHAR ,
                                               constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
                                                   references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
                                              JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                                              SHORT_CONTEXT VARCHAR(2500) NOT NULL,
                                              SERIALIZED_CONTEXT LONGVARCHAR ,
                                              constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
                                                  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ;
CREATE SEQUENCE BATCH_JOB_SEQ;

