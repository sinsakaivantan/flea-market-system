-- users
INSERT INTO users (name, email, password, role, enabled, banned)
VALUES
  ('出品者A', 'sellerA@example.com', '{noop}password', 'USER', TRUE, FALSE)
ON CONFLICT (email) DO UPDATE SET
  name = EXCLUDED.name,
  password = EXCLUDED.password,
  role = EXCLUDED.role,
  enabled = EXCLUDED.enabled,
  banned = EXCLUDED.banned;
INSERT INTO users (name, email, password, role, enabled, banned)
VALUES
  ('購入者B', 'xyz@example.com', '{noop}password', 'USER', TRUE, FALSE)
ON CONFLICT (email) DO UPDATE SET
  name = EXCLUDED.name,
  password = EXCLUDED.password,
  role = EXCLUDED.role,
  enabled = EXCLUDED.enabled,
  banned = EXCLUDED.banned;
INSERT INTO users (name, email, password, role, enabled, banned)
VALUES
  ('購入者D', 'buyerd@example.com', '{noop}password', 'USER', TRUE, FALSE)
ON CONFLICT (email) DO UPDATE SET
  name = EXCLUDED.name,
  password = EXCLUDED.password,
  role = EXCLUDED.role,
  enabled = EXCLUDED.enabled,
  banned = EXCLUDED.banned;
INSERT INTO users (name, email, password, role, enabled, banned)
VALUES
  ('運営者C', 'adminC@example.com', '{noop}adminpass', 'ADMIN', TRUE, FALSE)
ON CONFLICT (email) DO UPDATE SET
  name = EXCLUDED.name,
  password = EXCLUDED.password,
  role = EXCLUDED.role,
  enabled = EXCLUDED.enabled,
  banned = EXCLUDED.banned;

-- category
INSERT INTO category (name) VALUES
  ('本'), ('家電'), ('ファッション'), ('おもちゃ'),('文房具');

-- item
INSERT INTO item (user_id, name, description, price, category_id, status)
VALUES
  ((SELECT id FROM users WHERE email='sellerA@example.com'),
   'Javaプログラミング入門','初心者向けのJava入門書です。',1500.00,
   (SELECT id FROM category WHERE name='本'),'出品中'),

  ((SELECT id FROM users WHERE email='sellerA@example.com'),
   'ワイヤレスイヤホン','ノイズキャンセリング機能付き。',8000.00,
   (SELECT id FROM category WHERE name='家電'),'出品中');
