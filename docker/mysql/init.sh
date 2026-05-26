#!/bin/bash
set -e

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
  CREATE DATABASE IF NOT EXISTS aissue_content
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS aissue_notifications
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  GRANT ALL PRIVILEGES ON aissue_content.*       TO '${MYSQL_USER}'@'%';
  GRANT ALL PRIVILEGES ON aissue_notifications.* TO '${MYSQL_USER}'@'%';
  FLUSH PRIVILEGES;
EOSQL