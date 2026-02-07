-- =============================================
-- NETFLIZ WORKER DATABASE INITIALIZATION
-- Run on netfliz_analysis database (set by POSTGRES_DB env)
-- =============================================

-- =============================================
-- USERS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255),
    username VARCHAR(255),
    is_premium BOOLEAN DEFAULT FALSE,
    subscription_type VARCHAR(50) DEFAULT 'FREE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- =============================================
-- VIDEOS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS videos (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    genre VARCHAR(100),
    director VARCHAR(255),
    cast_members TEXT,
    duration INTEGER,
    rating DOUBLE PRECISION,
    release_date TIMESTAMP,
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    share_count BIGINT DEFAULT 0,
    last_viewed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_videos_genre ON videos(genre);
CREATE INDEX IF NOT EXISTS idx_videos_rating ON videos(rating);

-- =============================================
-- COMMENTS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    parent_comment_id BIGINT,
    like_count INTEGER DEFAULT 0,
    is_edited BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_video_id ON comments(video_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent ON comments(parent_comment_id);

-- =============================================
-- COMMENT LIKES TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    comment_id BIGINT,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comment_likes_user ON comment_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_comment_likes_comment ON comment_likes(comment_id);

-- =============================================
-- NOTIFICATIONS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    type VARCHAR(100),
    title VARCHAR(255),
    message VARCHAR(1000),
    deep_link VARCHAR(500),
    sent BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP,
    read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_sent ON notifications(sent);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);

-- =============================================
-- USER NOTIFICATION PREFERENCES TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    push_enabled BOOLEAN DEFAULT TRUE,
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- =============================================
-- PAYMENTS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) UNIQUE,
    user_id BIGINT,
    amount DOUBLE PRECISION,
    currency VARCHAR(10),
    subscription_type VARCHAR(50),
    payment_method VARCHAR(50),
    status VARCHAR(50),
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_user ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_transaction ON payments(transaction_id);

-- =============================================
-- SUBSCRIPTIONS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    type VARCHAR(50),
    status VARCHAR(50),
    start_date TIMESTAMP,
    expiry_date TIMESTAMP,
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_expiry ON subscriptions(expiry_date);

-- =============================================
-- LIKES TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_likes_user ON likes(user_id);
CREATE INDEX IF NOT EXISTS idx_likes_video ON likes(video_id);

-- =============================================
-- WATCHLIST TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS watchlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    video_id BIGINT,
    added_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_watchlist_user ON watchlist(user_id);
CREATE INDEX IF NOT EXISTS idx_watchlist_video ON watchlist(video_id);

-- =============================================
-- WATCH PROGRESS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS watch_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    video_id BIGINT,
    current_position INTEGER,
    percent_complete INTEGER,
    created_at TIMESTAMP,
    last_updated TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_watch_progress_user ON watch_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_watch_progress_video ON watch_progress(video_id);

-- =============================================
-- VIEW HISTORY TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS view_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    video_id BIGINT,
    video_title VARCHAR(255),
    view_start_time TIMESTAMP,
    watch_duration INTEGER,
    device_type VARCHAR(50),
    ip_address VARCHAR(50),
    quality VARCHAR(20),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_view_history_user ON view_history(user_id);
CREATE INDEX IF NOT EXISTS idx_view_history_video ON view_history(video_id);
CREATE INDEX IF NOT EXISTS idx_view_history_time ON view_history(view_start_time);

-- =============================================
-- USER PREFERENCES TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    genre VARCHAR(100),
    score DOUBLE PRECISION,
    interaction_count INTEGER,
    created_at TIMESTAMP,
    last_updated TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_user ON user_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_user_preferences_genre ON user_preferences(genre);

-- =============================================
-- RECOMMENDATIONS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    video_id BIGINT,
    type VARCHAR(50),
    score DOUBLE PRECISION,
    viewed BOOLEAN DEFAULT FALSE,
    clicked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommendations_user ON recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_video ON recommendations(video_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_type ON recommendations(type);

-- =============================================
-- SEARCH LOGS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS search_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    query VARCHAR(500),
    results_count INTEGER,
    timestamp TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_search_logs_user ON search_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_search_logs_timestamp ON search_logs(timestamp);

-- =============================================
-- ANALYTICS EVENTS TABLE
-- =============================================
CREATE TABLE IF NOT EXISTS analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100),
    user_id BIGINT,
    session_id VARCHAR(255),
    page VARCHAR(255),
    action VARCHAR(255),
    properties VARCHAR(2000),
    timestamp TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analytics_events_type ON analytics_events(event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_events_user ON analytics_events(user_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_session ON analytics_events(session_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_timestamp ON analytics_events(timestamp);
