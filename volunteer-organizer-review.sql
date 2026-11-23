-- 组织者评价：志愿者在活动结束后给组织者打分（1-5 星，1-2 星为差评）
CREATE TABLE IF NOT EXISTS organizer_review (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价主键',
  registration_id BIGINT NOT NULL COMMENT '对应报名记录（一人一条）',
  activity_id BIGINT NOT NULL COMMENT '活动 id',
  volunteer_id BIGINT NOT NULL COMMENT '评价人（志愿者）',
  organizer_id BIGINT NOT NULL COMMENT '被评价的组织者',
  rating TINYINT NOT NULL COMMENT '1-5 星，1-2 为差评',
  comment VARCHAR(200) DEFAULT NULL COMMENT '评价内容（≤120 字）',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_registration (registration_id),
  KEY idx_review_organizer (organizer_id, rating),
  KEY idx_review_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='志愿者对组织者的评价';
