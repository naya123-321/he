-- 修复 templates 表中文显示为 ??? 的常见原因：表/库字符集不是 utf8mb4
-- 说明：请在 MySQL 控制台执行本脚本（执行前建议先备份数据库）

-- 1) 让数据库默认字符集为 utf8mb4（可选，但推荐）
ALTER DATABASE pet_memorial CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 2) 将 templates 表整体转换为 utf8mb4
ALTER TABLE templates CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3) 显式修复关键字段（有些环境仅 CONVERT 不足）
ALTER TABLE templates
  MODIFY name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  MODIFY description VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  MODIFY category VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  MODIFY preview_image VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;



