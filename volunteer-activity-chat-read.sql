-- 活动群聊已读位置：每个用户在每个活动群记录自己读到哪条消息
CREATE TABLE IF NOT EXISTS activity_chat_read (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  last_read_id BIGINT NOT NULL DEFAULT 0,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_activity (user_id, activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 存量数据初始化：组织者按当前最后一条群消息作为已读位置
INSERT INTO activity_chat_read (user_id, activity_id, last_read_id, update_time)
SELECT a.organizer_id, a.id, COALESCE(MAX(c.id), 0), NOW()
FROM activity a
LEFT JOIN activity_chat c ON c.activity_id = a.id
WHERE a.organizer_id IS NOT NULL
GROUP BY a.id, a.organizer_id
ON DUPLICATE KEY UPDATE last_read_id = GREATEST(last_read_id, VALUES(last_read_id));

-- 存量数据初始化：审核通过的志愿者按当前最后一条群消息作为已读位置
INSERT INTO activity_chat_read (user_id, activity_id, last_read_id, update_time)
SELECT r.user_id, r.activity_id, COALESCE(MAX(c.id), 0), NOW()
FROM registration r
LEFT JOIN activity_chat c ON c.activity_id = r.activity_id
WHERE r.status = 'approved'
GROUP BY r.user_id, r.activity_id
ON DUPLICATE KEY UPDATE last_read_id = GREATEST(last_read_id, VALUES(last_read_id));
