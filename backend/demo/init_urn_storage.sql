-- 创建骨灰寄存表
CREATE TABLE IF NOT EXISTS `urn_storage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `user_id` BIGINT NOT NULL COMMENT '宠主ID',
  `pet_name` VARCHAR(50) NOT NULL COMMENT '宠物姓名',
  `pet_type` VARCHAR(50) DEFAULT NULL COMMENT '宠物类型',
  `urn_no` VARCHAR(50) NOT NULL COMMENT '骨灰盒编号',
  `storage_date` DATE NOT NULL COMMENT '寄存日期',
  `expiry_date` DATE NOT NULL COMMENT '到期日期',
  `storage_period` INT NOT NULL COMMENT '寄存期限（月）',
  `storage_location` VARCHAR(100) DEFAULT NULL COMMENT '寄存位置',
  `status` INT NOT NULL DEFAULT 0 COMMENT '状态：0-寄存中，1-已取回，2-已到期',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expiry_date` (`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骨灰寄存表';


