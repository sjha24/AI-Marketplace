USE ai_marketplace;

DROP DATABASE ai_marketplace;

CREATE DATABASE ai_marketplace CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT installed_rank, version, description, checksum, success
FROM flyway_schema_history
ORDER BY installed_rank;


UPDATE flyway_schema_history
SET checksum = 1622404055
WHERE version = '1';