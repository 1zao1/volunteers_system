-- 予光 · 活动群聊表（每个活动的组织者 + 报名志愿者）
CREATE TABLE IF NOT EXISTS `activity_chat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `sender_id` BIGINT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
