#!/bin/bash
set -eu

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
  CREATE DATABASE IF NOT EXISTS authentication CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS flashsale CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS payment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  CREATE USER IF NOT EXISTS 'authentication_user'@'%' IDENTIFIED WITH caching_sha2_password BY '${AUTH_DB_PASSWORD}';
  CREATE USER IF NOT EXISTS 'notification_user'@'%' IDENTIFIED WITH caching_sha2_password BY '${NOTIFICATION_DB_PASSWORD}';
  CREATE USER IF NOT EXISTS 'flashsale_user'@'%' IDENTIFIED WITH caching_sha2_password BY '${FLASHSALE_DB_PASSWORD}';
  CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED WITH caching_sha2_password BY '${PAYMENT_DB_PASSWORD}';
  CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED WITH caching_sha2_password BY '${MYSQL_PASSWORD}';

  GRANT ALL PRIVILEGES ON authentication.* TO 'authentication_user'@'%';
  GRANT ALL PRIVILEGES ON notification.* TO 'notification_user'@'%';
  GRANT ALL PRIVILEGES ON flashsale.* TO 'flashsale_user'@'%';
  GRANT ALL PRIVILEGES ON payment.* TO 'payment_user'@'%';

  GRANT ALL PRIVILEGES ON authentication.* TO '${MYSQL_USER}'@'%';
  GRANT ALL PRIVILEGES ON notification.* TO '${MYSQL_USER}'@'%';
  GRANT ALL PRIVILEGES ON flashsale.* TO '${MYSQL_USER}'@'%';
  GRANT ALL PRIVILEGES ON payment.* TO '${MYSQL_USER}'@'%';
  FLUSH PRIVILEGES;
EOSQL
