# 智能套餐推荐（Python FastAPI）

## 1. 安装依赖

在项目根目录执行（Windows PowerShell 也可）：

```bash
cd backend/recommendation_service
python -m pip install -r requirements.txt
```

## 2. 启动服务

```bash
uvicorn main:app --host 127.0.0.1 --port 8001
```

启动后 Spring Boot 会通过 `recommend.python.url` 调用：
- 默认：`http://127.0.0.1:8001/recommend`

## 3. 说明

- 推荐逻辑：**协同过滤（相似用户选择频率） + 规则引擎（宠物类型/年龄/离世原因）**
- 如果未启动 Python 服务，后端会自动降级为 **规则引擎 + 历史热度**，前端仍可展示推荐结果与解释。


