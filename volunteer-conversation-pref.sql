-- 会话置顶 / 删除偏好：只影响当前用户自己的会话列表，不改动聊天记录
CREATE TABLE IF NOT EXISTS conversation_pref (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  conv_type VARCHAR(10) NOT NULL,
  target_id BIGINT NOT NULL,
  pinned TINYINT(1) NOT NULL DEFAULT 0,
  deleted_time DATETIME NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_conv (user_id, conv_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
