-- 为 service_types 表添加 isRecommended 字段
-- 如果字段已存在，此脚本会报错，可以忽略

ALTER TABLE service_types 
ADD COLUMN is_recommended BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否推荐：true-推荐 false-标准';

-- 如果需要将某些现有套餐设置为推荐，可以执行以下SQL（可选）
-- UPDATE service_types SET is_recommended = TRUE WHERE id = 1; -- 示例：将ID为1的套餐设置为推荐


