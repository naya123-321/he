# 添加模板说明

## 添加"经典简约 · 永恒的陪伴"模板

### 方法一：直接执行SQL脚本（推荐）

1. 打开MySQL客户端（如Navicat、MySQL Workbench或命令行）
2. 连接到数据库 `pet_memorial`
3. 执行SQL脚本：

```bash
# 方式1：使用命令行
mysql -u root -p pet_memorial < init_template_classic.sql

# 方式2：在MySQL客户端中直接执行
source init_template_classic.sql
```

或者直接复制以下SQL语句执行：

```sql
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
```

### 方法二：通过管理界面添加

1. 登录管理员或服务人员账号
2. 访问"模板管理"页面
3. 点击"添加模板"按钮
4. 填写以下信息：
   - **模板名称**：经典简约 · 永恒的陪伴
   - **分类**：经典风格
   - **描述**：庄重、简洁、永恒的纪念册模板，采用纯白背景和深蓝灰强调色，适合表达对宠物的永恒怀念。
   - **样式配置**：复制以下JSON
   - **布局配置**：复制以下JSON

#### 样式配置JSON：
```json
{
  "templateName": "经典简约",
  "version": "1.0",
  "theme": "永恒的陪伴",
  "mood": "庄重、简洁、永恒",
  "colorPalette": {
    "primary": "#FFFFFF",
    "secondary": "#F5F5F5",
    "accent": "#2C3E50",
    "textPrimary": "#333333",
    "textSecondary": "#666666",
    "border": "#E0E0E0",
    "highlight": "#3498DB"
  },
  "typography": {
    "fontFamilies": {
      "title": "SimHei, 黑体, Microsoft YaHei, sans-serif",
      "body": "SimSun, 宋体, Microsoft JhengHei, serif",
      "quote": "FangSong, 仿宋, STFangsong, serif"
    },
    "sizes": {
      "h1": "28px",
      "h2": "22px",
      "h3": "18px",
      "body": "16px",
      "small": "14px"
    },
    "lineHeight": "1.6"
  },
  "effects": {
    "borderRadius": "4px",
    "shadow": "0 2px 8px rgba(0,0,0,0.08)",
    "imageFilter": "none",
    "transition": "all 0.3s ease"
  },
  "components": {
    "photoFrame": "clean_white",
    "textBox": "light_grey_bg",
    "separator": "thin_grey_line",
    "ornament": "simple_dot"
  },
  "printOptimized": true,
  "recommendedPaper": "哑光铜版纸",
  "colorMode": "CMYK"
}
```

#### 布局配置JSON：
```json
{
  "columns": 1,
  "spacing": 20,
  "align": "center",
  "layoutType": "single_column",
  "sections": [
    {
      "type": "cover",
      "height": "100vh",
      "background": "#FFFFFF"
    },
    {
      "type": "photo_gallery",
      "columns": 3,
      "spacing": 15,
      "padding": 20
    },
    {
      "type": "text_content",
      "maxWidth": "800px",
      "padding": 40
    },
    {
      "type": "timeline",
      "orientation": "vertical",
      "spacing": 30
    }
  ]
}
```

### 模板特点

- **主题**：永恒的陪伴
- **风格**：庄重、简洁、永恒
- **配色方案**：
  - 主色：纯白 (#FFFFFF)
  - 辅助色：浅灰 (#F5F5F5)
  - 强调色：深蓝灰 (#2C3E50)
  - 高亮色：亮蓝 (#3498DB)
- **字体**：
  - 标题：黑体
  - 正文：宋体
  - 引用：仿宋
- **布局**：单栏居中布局，包含封面、照片墙、文字内容和时间线
- **打印优化**：支持CMYK色彩模式，推荐使用哑光铜版纸

### 验证

添加成功后，可以在以下位置查看：
1. 管理端/服务端：模板管理页面
2. 宠主端：创建纪念册时的模板选择页面（经典风格分类下）


