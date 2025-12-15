-- 初始化服务类型数据
-- 如果表中已有数据，可以先清空：DELETE FROM service_types;

INSERT INTO service_types (name, description, price, duration, status, create_time, update_time) 
VALUES 
('基础套餐', '为您的宠物提供基本的殡葬服务，包括遗体处理、告别仪式和骨灰存放', 999.00, 60, 1, NOW(), NOW()),
('尊享套餐', '为您的宠物提供全方位的殡葬服务，包括豪华告别仪式和精美纪念册', 1999.00, 90, 1, NOW(), NOW()),
('定制套餐', '根据您的特殊需求定制专属服务，提供个性化的告别仪式和纪念方案', 2999.00, 120, 1, NOW(), NOW());


