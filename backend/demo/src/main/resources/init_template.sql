-- 初始化模板数据
-- 经典简约 · 永恒的陪伴模板
-- 如果表中已有同名模板，可以先删除：DELETE FROM templates WHERE name = '经典简约 · 永恒的陪伴';

INSERT INTO templates (
    name,
    description,
    category,
    preview_image,
    style_config,
    layout_config,
    status,
    created_by,
    create_time,
    update_time
) VALUES (
    '经典简约 · 永恒的陪伴',
    '庄重、简洁、永恒的纪念册模板，采用纯白背景和深蓝灰强调色，适合表达对宠物的永恒怀念。',
    'classic',
    NULL,
    '{"templateName":"经典简约","version":"1.0","theme":"永恒的陪伴","mood":"庄重、简洁、永恒","colorPalette":{"primary":"#FFFFFF","secondary":"#F5F5F5","accent":"#2C3E50","textPrimary":"#333333","textSecondary":"#666666","border":"#E0E0E0","highlight":"#3498DB"},"typography":{"fontFamilies":{"title":"SimHei, 黑体, Microsoft YaHei, sans-serif","body":"SimSun, 宋体, Microsoft JhengHei, serif","quote":"FangSong, 仿宋, STFangsong, serif"},"sizes":{"h1":"28px","h2":"22px","h3":"18px","body":"16px","small":"14px"},"lineHeight":"1.6"},"effects":{"borderRadius":"4px","shadow":"0 2px 8px rgba(0,0,0,0.08)","imageFilter":"none","transition":"all 0.3s ease"},"components":{"photoFrame":"clean_white","textBox":"light_grey_bg","separator":"thin_grey_line","ornament":"simple_dot"},"printOptimized":true,"recommendedPaper":"哑光铜版纸","colorMode":"CMYK"}',
    '{"columns":1,"spacing":20,"align":"center","layoutType":"single_column","sections":[{"type":"cover","height":"100vh","background":"#FFFFFF"},{"type":"photo_gallery","columns":3,"spacing":15,"padding":20},{"type":"text_content","maxWidth":"800px","padding":40},{"type":"timeline","orientation":"vertical","spacing":30}]}',
    1,
    1,
    NOW(),
    NOW()
);

