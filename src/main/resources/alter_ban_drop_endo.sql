-- ban テーブルに endo と end_date が両方ある場合、endo を削除して end_date のみにする
-- 実行例（PostgreSQL）:
--   psql -h localhost -p 5432 -U postgres -d fleamarketdb -f alter_ban_drop_endo.sql
ALTER TABLE ban DROP COLUMN IF EXISTS endo;
