-- ========== CLEAN DROP (依存順) ==========
-- 警告: このDROP文は本番環境では実行しないでください！
-- データベースの全データが削除されます。
-- 開発時のみ、手動で実行してください。
-- 
-- DROP TABLE IF EXISTS chat CASCADE;
-- DROP TABLE IF EXISTS favorite_item CASCADE;
-- DROP TABLE IF EXISTS review CASCADE;
-- DROP TABLE IF EXISTS app_order CASCADE;
-- DROP TABLE IF EXISTS item CASCADE;
-- DROP TABLE IF EXISTS category CASCADE;
-- DROP TABLE IF EXISTS user_complaint CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;

-- ========== CREATE ==========
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,            -- 'USER' / 'ADMIN'
  line_notify_token VARCHAR(255),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,

  -- ★ BAN系（最初から持たせる）
  banned BOOLEAN NOT NULL DEFAULT FALSE,
  ban_reason TEXT,
  banned_at TIMESTAMP,
  banned_by_admin_id INT
);

CREATE TABLE category (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE item (
  id SERIAL PRIMARY KEY,
  user_id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price NUMERIC(10,2) NOT NULL,
  category_id INT,
  status VARCHAR(20) DEFAULT '出品中',
  image_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE app_order (
  id SERIAL PRIMARY KEY,
  item_id INT NOT NULL,
  buyer_id INT NOT NULL,
  price NUMERIC(10,2) NOT NULL,
  status VARCHAR(20) DEFAULT '購入済',
  payment_intent_id VARCHAR(128),               -- ★ StripeのPI ID
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES item(id),
  FOREIGN KEY (buyer_id) REFERENCES users(id)
);

CREATE TABLE chat (
  id SERIAL PRIMARY KEY,
  item_id INT NOT NULL,
  sender_id INT NOT NULL,
  message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES item(id),
  FOREIGN KEY (sender_id) REFERENCES users(id)
);

CREATE TABLE favorite_item (
  id SERIAL PRIMARY KEY,
  user_id INT NOT NULL,
  item_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, item_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (item_id) REFERENCES item(id)
);

CREATE TABLE review (
  id SERIAL PRIMARY KEY,
  order_id INT NOT NULL UNIQUE,
  reviewer_id INT NOT NULL,
  seller_id INT NOT NULL,
  item_id INT NOT NULL,
  rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES app_order(id),
  FOREIGN KEY (reviewer_id) REFERENCES users(id),
  FOREIGN KEY (seller_id) REFERENCES users(id),
  FOREIGN KEY (item_id) REFERENCES item(id)
);

CREATE TABLE user_complaint (
  id SERIAL PRIMARY KEY,
  reported_user_id INT NOT NULL,
  reporter_user_id INT NOT NULL,
  reason TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (reported_user_id) REFERENCES users(id),
  FOREIGN KEY (reporter_user_id) REFERENCES users(id)
);

-- ========== INDEX ==========
CREATE INDEX IF NOT EXISTS idx_users_banned           ON users(banned);
CREATE INDEX IF NOT EXISTS idx_users_banned_by        ON users(banned_by_admin_id);

CREATE INDEX IF NOT EXISTS idx_item_user_id           ON item(user_id);
CREATE INDEX IF NOT EXISTS idx_item_category_id       ON item(category_id);

CREATE INDEX IF NOT EXISTS idx_order_item_id          ON app_order(item_id);
CREATE INDEX IF NOT EXISTS idx_order_buyer_id         ON app_order(buyer_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_order_pi         ON app_order(payment_intent_id);

CREATE INDEX IF NOT EXISTS idx_chat_item_id           ON chat(item_id);
CREATE INDEX IF NOT EXISTS idx_chat_sender_id         ON chat(sender_id);

CREATE INDEX IF NOT EXISTS idx_fav_user_id            ON favorite_item(user_id);
CREATE INDEX IF NOT EXISTS idx_fav_item_id            ON favorite_item(item_id);

CREATE INDEX IF NOT EXISTS idx_review_order_id        ON review(order_id);

CREATE INDEX IF NOT EXISTS idx_uc_reported            ON user_complaint(reported_user_id);
CREATE INDEX IF NOT EXISTS idx_uc_reporter            ON user_complaint(reporter_user_id);
