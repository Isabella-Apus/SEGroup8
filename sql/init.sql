-- Manual database initialization entrypoint.
--
-- Run this file from the repository root:
--   mysql -uroot -p segroup8_platform < sql/init.sql
--
-- Keep this file small and delegate table/data definitions to the same
-- resources used by Spring Boot, so manual initialization does not drift
-- from the application schema.

DROP DATABASE IF EXISTS segroup8_platform;
CREATE DATABASE segroup8_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE segroup8_platform;

SOURCE backend/src/main/resources/schema.sql;
SOURCE backend/src/main/resources/data.sql;
